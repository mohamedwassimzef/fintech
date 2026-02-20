# Modern Input Controls - Developer Guide

## 📋 Overview

The application uses a **modern, type-safe input control system** that provides real-time validation and formatting for all TextField components. This prevents invalid data entry at the UI level before it reaches business logic.

---

## 🎯 Key Features

### ✅ **Type-Safe Validation**
- Enum-based input types for compile-time safety
- No magic strings or scattered regex patterns
- Centralized validation rules

### ✅ **Real-Time Feedback**
- Invalid characters are rejected immediately as user types
- Maximum length enforcement
- Automatic prompt text hints

### ✅ **Extensible Architecture**
- Easy to add new input types
- Reusable across multiple fields
- Clean separation of concerns

### ✅ **User-Friendly**
- Helpful placeholder text
- Clear constraints communicated to users
- No frustrating post-submission errors

---

## 🏗️ Architecture

### Component Structure

```
MainController
├── applyInputControls()              // Entry point - called during initialization
├── InputControlType (enum)           // Defines validation patterns
├── applyTextControl()                // Applies validation to a TextField
└── getPromptText()                   // Generates user-friendly hints
```

---

## 📦 Input Control Types

### Available Types:

| Type | Pattern | Use Case | Example |
|------|---------|----------|---------|
| `TEXT` | `[\p{L}\p{N}\s.,'\\-_()]*` | Names, descriptions | "John's Company (Ltd.)" |
| `ALPHANUMERIC` | `[A-Za-z0-9]*` | IDs, codes | "ABC123" |
| `ALPHANUMERIC_DASH` | `[A-Za-z0-9\\-_]*` | Contract numbers | "IC-2026-001" |
| `INTEGER` | `\\d*` | Whole numbers | "12345" |
| `DECIMAL` | `\\d*(\\.\\d*)?` | Amounts, prices | "1500.50" |
| `PHONE` | `[\\d\\s()\\-+]*` | Phone numbers | "+1 (555) 123-4567" |
| `EMAIL` | `[A-Za-z0-9@._\\-]*` | Email addresses | "user@example.com" |

### Pattern Explanations:

- **TEXT**: Unicode letters, numbers, spaces, and common punctuation
- **ALPHANUMERIC**: Only letters (A-Z, a-z) and digits (0-9)
- **ALPHANUMERIC_DASH**: Letters, numbers, dashes, and underscores
- **INTEGER**: Only digits (0-9)
- **DECIMAL**: Digits with optional single decimal point
- **PHONE**: Digits, spaces, parentheses, dashes, plus sign
- **EMAIL**: Characters commonly used in email addresses

---

## 💻 Implementation

### 1. How Input Controls Are Applied

**Entry Point** (called automatically during initialization):

```java
@FXML
private void initialize() {
    // ... other initialization code ...
    applyInputControls();  // ← Applies all input controls
}
```

### 2. Configuration Method

```java
private void applyInputControls() {
    // Asset Form Controls
    applyTextControl(AssName, InputControlType.TEXT, 100);
    applyTextControl(AssType, InputControlType.TEXT, 50);
    applyTextControl(AssValue, InputControlType.DECIMAL, 15);
    applyTextControl(AssDesc, InputControlType.TEXT, 255);
    applyTextControl(AssUser, InputControlType.INTEGER, 10);

    // Contract Form Controls
    applyTextControl(contractNumberField, InputControlType.ALPHANUMERIC_DASH, 50);
    applyTextControl(contractAssetIdField, InputControlType.INTEGER, 10);
    applyTextControl(contractUserIdField, InputControlType.INTEGER, 10);
    applyTextControl(contractPremiumField, InputControlType.DECIMAL, 15);
    applyTextControl(contractCoverageField, InputControlType.DECIMAL, 15);
    applyTextControl(contractApprovedByField, InputControlType.INTEGER, 10);
}
```

### 3. Core Logic

The `applyTextControl()` method creates a `TextFormatter` that:

1. **Validates each character** as the user types
2. **Enforces maximum length** (if specified)
3. **Rejects invalid input** immediately
4. **Sets helpful prompt text** for user guidance

```java
private void applyTextControl(TextField field, InputControlType type, int maxLength) {
    UnaryOperator<TextFormatter.Change> filter = change -> {
        String newText = change.getControlNewText();
        
        // Check max length constraint
        if (maxLength > 0 && newText.length() > maxLength) {
            return null;  // Reject change
        }
        
        // Check pattern constraint
        if (!newText.matches(type.getRegex())) {
            return null;  // Reject change
        }
        
        return change;  // Accept change
    };

    field.setTextFormatter(new TextFormatter<>(filter));
    field.setPromptText(getPromptText(type, maxLength));
}
```

---

## 🔧 How to Add New Input Types

### Step 1: Add to InputControlType Enum

```java
private enum InputControlType {
    // ... existing types ...
    POSTAL_CODE("[A-Z0-9\\s\\-]*"),  // Example: postal code
    // ... more types ...
}
```

### Step 2: Add Prompt Text (Optional)

```java
private String getPromptText(InputControlType type, int maxLength) {
    return switch (type) {
        // ... existing cases ...
        case POSTAL_CODE -> "Enter postal code";
        default -> "Max " + maxLength + " characters";
    };
}
```

### Step 3: Apply to Fields

```java
private void applyInputControls() {
    applyTextControl(postalCodeField, InputControlType.POSTAL_CODE, 10);
}
```

---

## 📊 Current Field Configurations

### Asset Form:

| Field | Type | Max Length | Description |
|-------|------|------------|-------------|
| `AssName` | TEXT | 100 | Asset name (letters, numbers, punctuation) |
| `AssType` | TEXT | 50 | Asset type/category |
| `AssValue` | DECIMAL | 15 | Asset monetary value |
| `AssDesc` | TEXT | 255 | Asset description |
| `AssUser` | INTEGER | 10 | User ID (numbers only) |

### Contract Form:

| Field | Type | Max Length | Description |
|-------|------|------------|-------------|
| `contractNumberField` | ALPHANUMERIC_DASH | 50 | Contract number (e.g., IC-2026-001) |
| `contractAssetIdField` | INTEGER | 10 | Asset ID reference |
| `contractUserIdField` | INTEGER | 10 | User ID reference |
| `contractPremiumField` | DECIMAL | 15 | Premium amount |
| `contractCoverageField` | DECIMAL | 15 | Coverage amount |
| `contractApprovedByField` | INTEGER | 10 | Approver ID |

---

## 🎨 User Experience

### What Users See:

1. **Placeholder Text**: Helpful hints in empty fields
   - Example: "Enter integer (max 10 digits)"

2. **Instant Validation**: Invalid characters are rejected immediately
   - User tries to type "abc" in an INTEGER field → Nothing happens
   - User types "123" → Accepted immediately

3. **Length Limits**: Fields stop accepting input at max length
   - No frustrating "too long" errors after submission

4. **Visual Consistency**: All fields follow the same validation approach

---

## 🔄 Migration from Legacy Code

### Old Approach (Deprecated):

```java
// ❌ Old way - repetitive, no max length, no hints
applyIntegerOnly(field1);
applyIntegerOnly(field2);
applyDecimalOnly(field3);
```

### New Approach (Modern):

```java
// ✅ New way - declarative, configurable, user-friendly
applyTextControl(field1, InputControlType.INTEGER, 10);
applyTextControl(field2, InputControlType.INTEGER, 10);
applyTextControl(field3, InputControlType.DECIMAL, 15);
```

### Legacy Methods Preserved:

The old methods (`applyIntegerOnly`, `applyDecimalOnly`, `applyAlphanumericWithDash`) are marked as `@Deprecated` but still work. They internally call the new `applyTextControl()` method for backward compatibility.

---

## 🧪 Testing Input Controls

### Manual Testing Checklist:

1. **Type Valid Data**: Should be accepted
2. **Type Invalid Characters**: Should be rejected silently
3. **Exceed Max Length**: Should stop accepting input
4. **Check Placeholder Text**: Should show helpful hints
5. **Copy-Paste Test**: Large paste should be truncated to max length
6. **Clear and Retype**: Should work consistently

### Example Test Cases:

#### Integer Field (User ID):
- ✅ Type "123" → Accepted
- ❌ Type "abc" → Rejected
- ❌ Type "12.5" → Rejected
- ✅ Type "9999999999" (if limit is 10) → Accepted
- ❌ Type "99999999999" (if limit is 10) → Last digit rejected

#### Decimal Field (Premium):
- ✅ Type "1500.50" → Accepted
- ✅ Type "1500" → Accepted
- ❌ Type "abc" → Rejected
- ❌ Type "15.50.25" → Second decimal point rejected

#### Contract Number Field:
- ✅ Type "IC-2026-001" → Accepted
- ✅ Type "CONTRACT_123" → Accepted
- ❌ Type "IC 2026 001" → Spaces rejected
- ❌ Type "IC@2026" → Special char rejected

---

## 📈 Benefits

### For Developers:
- **Less Code Duplication**: One method handles all validations
- **Type Safety**: Enum prevents typos in validation types
- **Easy Maintenance**: Change validation in one place
- **Self-Documenting**: Clear what validation each field uses

### For Users:
- **Immediate Feedback**: No waiting for form submission
- **Clear Constraints**: Placeholder text shows requirements
- **Frustration-Free**: Can't enter invalid data
- **Professional UX**: Consistent behavior across all fields

### For Business:
- **Data Quality**: Invalid data never reaches database
- **Reduced Errors**: Fewer validation errors to handle
- **Better UX**: Users complete forms faster
- **Maintainability**: Easy to add/modify validations

---

## 🚀 Best Practices

### DO:
✅ Use descriptive InputControlType names
✅ Set appropriate max lengths based on database schema
✅ Add helpful prompt text for complex patterns
✅ Test with edge cases (empty, max length, special chars)
✅ Document any custom validation patterns

### DON'T:
❌ Use overly restrictive patterns that frustrate users
❌ Set max length too small for valid use cases
❌ Forget to handle nullable fields (use Optional or allow empty)
❌ Mix input control with business logic validation
❌ Skip testing with real user scenarios

---

## 🔮 Future Enhancements

Potential improvements to consider:

1. **Visual Feedback**: Highlight fields in red when invalid input attempted
2. **Tooltips**: Show validation rules on hover
3. **Custom Error Messages**: Display specific reasons for rejection
4. **Regex Builder**: GUI tool to test patterns before adding
5. **Localization**: Support for different locale patterns (dates, numbers)
6. **Async Validation**: Check database for duplicates while typing
7. **Smart Suggestions**: Autocomplete based on validation type

---

## 📚 Related Documentation

- [Insured Asset Entity Documentation](./insured-asset.md)
- [Style Guide](./STYLE_GUIDE.md)
- [JavaFX TextFormatter Documentation](https://openjfx.io/javadoc/17/javafx.controls/javafx/scene/control/TextFormatter.html)

---

## 👥 Contributing

When adding new validations:

1. Add to `InputControlType` enum with clear regex
2. Update `getPromptText()` with helpful message
3. Apply to fields in `applyInputControls()`
4. Update this documentation
5. Test thoroughly with edge cases

---

**Last Updated**: February 19, 2026
**Version**: 2.0 (Modern Input Controls)
**Maintainer**: Development Team

