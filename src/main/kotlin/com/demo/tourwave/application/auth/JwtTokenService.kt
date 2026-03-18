package com.demo.tourwave.application.auth

import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class JwtTokenService(
    secret: String,
    private val accessTokenTtlSeconds: Long,
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
) {
    private val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun issueAccessToken(
        userId: Long,
        roles: Set<ActorRole>,
        orgId: Long?,
    ): String {
        val now = clock.instant()
        val payload =
            linkedMapOf(
                "sub" to userId.toString(),
                "roles" to roles.map { it.name },
                "orgId" to orgId,
                "iat" to now.epochSecond,
                "exp" to now.plusSeconds(accessTokenTtlSeconds).epochSecond,
            )
        return encode(payload)
    }

    fun parse(token: String): AccessTokenClaims {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw unauthorized("invalid jwt format")
        }

        val signingInput = "${parts[0]}.${parts[1]}"
        val expectedSignature = sign(signingInput)
        if (expectedSignature != parts[2]) {
            throw unauthorized("jwt signature is invalid")
        }

        val payloadJson =
            runCatching {
                String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
            }.getOrElse {
                throw unauthorized("jwt payload is invalid")
            }
        val payload =
            runCatching {
                objectMapper.readValue(payloadJson, Map::class.java)
            }.getOrElse {
                throw unauthorized("jwt payload is invalid")
            }

        val userId = (payload["sub"] as? String)?.toLongOrNull() ?: throw unauthorized("jwt subject is invalid")
        val iat = (payload["iat"] as? Number)?.toLong() ?: throw unauthorized("jwt iat is invalid")
        val exp = (payload["exp"] as? Number)?.toLong() ?: throw unauthorized("jwt exp is invalid")
        val nowEpoch = clock.instant().epochSecond
        if (exp <= nowEpoch) {
            throw unauthorized("jwt is expired")
        }
        val roles =
            ((payload["roles"] as? Collection<*>) ?: emptyList<Any>())
                .mapNotNull { ActorRole.parseActorRole(it?.toString()) ?: ActorRole.parseOrgRole(it?.toString()) }
                .toSet()
                .ifEmpty { setOf(ActorRole.USER) }
        val orgId = (payload["orgId"] as? Number)?.toLong()

        return AccessTokenClaims(
            userId = userId,
            roles = roles,
            orgId = orgId,
            issuedAtEpochSeconds = iat,
            expiresAtEpochSeconds = exp,
        )
    }

    private fun encode(payload: Map<String, Any?>): String {
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        val body = encoder.encodeToString(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$header.$body"
        val signature = sign(signingInput)
        return "$signingInput.$signature"
    }

    private fun sign(signingInput: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        return encoder.encodeToString(mac.doFinal(signingInput.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun unauthorized(message: String): DomainException =
        DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = message,
        )
}
