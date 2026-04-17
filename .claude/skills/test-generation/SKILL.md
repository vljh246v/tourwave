---
name: test-generation
description: Write tests using TDD methodology
type: testing
---

# Test Generation Skill

Use this skill for writing tests before implementing new features (TDD) or strengthening tests for existing code.

## TDD Flow

```
1. RED   — write a failing test first
2. GREEN — write the minimum implementation to pass the test
3. REFACTOR — remove duplication, improve structure
```

## Test Writing Principles

### Test Naming Convention
```
// Kotlin
@Test
fun `should return error when user not found`() { }

// Python
def test_should_return_error_when_user_not_found(): pass

// TypeScript
it('should return error when user not found', () => {})
```

Test name format: `should [result] when [condition]`

### AAA Pattern
```kotlin
@Test
fun `should calculate total price including tax`() {
  // Arrange
  val item = Item(price = 1000, quantity = 2)

  // Act
  val result = PriceCalculator.calculate(item, taxRate = 0.1)

  // Assert
  assertEquals(2200, result.totalPrice)
}
```

## Test Checklist

### Basics
- [ ] Happy Path (normal case) covered
- [ ] Error Path (error case) covered
- [ ] Edge Case (boundary values) covered

### Edge Case Catalog
- null / empty / blank input
- Min / max values (Int.MIN_VALUE, etc.)
- Empty list / single item / large collection
- Timeout / network error
- Unauthorized user

### Test Isolation
- [ ] Can each test run independently?
- [ ] Is there no shared state between tests?
- [ ] Are external dependencies (DB, API) replaced with mocks/stubs?

## Language-Specific Patterns

### Kotlin/Spring Boot
```kotlin
@SpringBootTest
@Transactional
class UserServiceTest {
  @Autowired lateinit var userService: UserService
  @MockBean lateinit var userRepository: UserRepository

  @Test
  fun `should find user by id`() {
    val user = User(id = 1L, name = "testUser")
    given(userRepository.findById(1L)).willReturn(Optional.of(user))

    val result = userService.findById(1L)

    assertNotNull(result)
    assertEquals("testUser", result.name)
  }
}
```

### TypeScript
```typescript
describe('UserService', () => {
  let service: UserService;
  let mockRepo: jest.Mocked<UserRepository>;

  beforeEach(() => {
    mockRepo = { findById: jest.fn() } as any;
    service = new UserService(mockRepo);
  });

  it('should find user by id', async () => {
    mockRepo.findById.mockResolvedValue({ id: 1, name: 'testUser' });
    const result = await service.findById(1);
    expect(result.name).toBe('testUser');
  });
});
```

## Check Failure Patterns

Before starting, review `logs/trends/failure-patterns.md`
to identify commonly failing test patterns.
