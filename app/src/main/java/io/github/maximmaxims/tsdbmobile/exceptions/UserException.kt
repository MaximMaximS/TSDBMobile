package io.github.maximmaxims.tsdbmobile.exceptions

import io.github.maximmaxims.tsdbmobile.utils.ErrorType

class UserException(val type: ErrorType) : TSDBException()