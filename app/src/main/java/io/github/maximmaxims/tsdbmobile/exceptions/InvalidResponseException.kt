package io.github.maximmaxims.tsdbmobile.exceptions

import okhttp3.Response

class InvalidResponseException(val response: Response, val isId: Boolean) : TSDBException()