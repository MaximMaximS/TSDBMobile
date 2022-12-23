package io.github.maximmaxims.tsdbmobile.utils

enum class ErrorType(val value: Boolean) {
    NO_CREDS(false), API_FAIL(false), INVALID_URL(false), FAILED_TO_PARSE(true), NO_EPISODES_FOUND(false), ID_MISMATCH(
        true
    ),
    EMPTY_CREDS(false), EMPTY_TITLE(false), EMPTY_SE(false)
}