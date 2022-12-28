package io.github.maximmaxims.tsdbmobile.exceptions

import okhttp3.Response

class ResponseException(val response: Response) : TSDBException()