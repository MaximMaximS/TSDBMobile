package io.github.maximmaxims.tsdbmobile.exceptions

import java.io.IOException


class RequestFailedException(val e: IOException) : TSDBException()