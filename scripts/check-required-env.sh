#!/usr/bin/env bash

set -euo pipefail

profile="${1:-}"

if [[ -z "$profile" ]]; then
  echo "[ERROR][E_USAGE] Usage: $0 <alpha|beta|real>"
  exit 2
fi

required_keys=()
case "$profile" in
  alpha)
    required_keys=(
      ALPHA_DB_URL
      ALPHA_DB_USERNAME
      ALPHA_DB_PASSWORD
      ALPHA_PAYMENT_BASE_URL
      ALPHA_PAYMENT_API_KEY
      ALPHA_NOTIFICATION_BASE_URL
      ALPHA_NOTIFICATION_API_KEY
      ALPHA_ASSET_BASE_URL
      ALPHA_ASSET_ACCESS_TOKEN
    )
    ;;
  beta)
    required_keys=(
      BETA_DB_URL
      BETA_DB_USERNAME
      BETA_DB_PASSWORD
      BETA_PAYMENT_BASE_URL
      BETA_PAYMENT_API_KEY
      BETA_NOTIFICATION_BASE_URL
      BETA_NOTIFICATION_API_KEY
      BETA_ASSET_BASE_URL
      BETA_ASSET_ACCESS_TOKEN
    )
    ;;
  real)
    required_keys=(
      REAL_DB_URL
      REAL_DB_USERNAME
      REAL_DB_PASSWORD
      REAL_PAYMENT_BASE_URL
      REAL_PAYMENT_API_KEY
      REAL_NOTIFICATION_BASE_URL
      REAL_NOTIFICATION_API_KEY
      REAL_ASSET_BASE_URL
      REAL_ASSET_ACCESS_TOKEN
    )
    ;;
  *)
    echo "[ERROR][E_UNSUPPORTED_PROFILE] Unsupported profile '$profile'. Expected one of: alpha, beta, real"
    exit 2
    ;;
esac

missing_keys=()
for key in "${required_keys[@]}"; do
  value="${!key:-}"
  if [[ -z "${value//[[:space:]]/}" ]]; then
    missing_keys+=("$key")
  fi
done

if (( ${#missing_keys[@]} > 0 )); then
  echo "[ERROR][E_MISSING_REQUIRED_ENV] Missing required environment variables for profile '$profile': ${missing_keys[*]}"
  exit 1
fi

echo "[OK] All required environment variable keys are present for profile '$profile'."
