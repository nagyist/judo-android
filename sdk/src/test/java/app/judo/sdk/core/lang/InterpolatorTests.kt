/*
 * Copyright (c) 2020-present, Rover Labs, Inc. All rights reserved.
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Rover.
 *
 * This copyright notice shall be included in all copies or substantial portions of
 * the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package app.judo.sdk.core.lang

import app.judo.sdk.core.data.DataContext
import app.judo.sdk.core.data.dataContextOf
import app.judo.sdk.core.data.emptyDataContext
import app.judo.sdk.core.implementations.InterpolatorImpl
import app.judo.sdk.core.implementations.StringExpressionException
import app.judo.sdk.utils.TestLoggerImpl
import org.junit.Test
import java.lang.Exception
import java.util.*

class InterpolatorTests {

    private val logger = TestLoggerImpl()
    private val interpolator = InterpolatorImpl(loggerSupplier = { logger })
    private val nonInterpolatedString = "NON_INTERPOLATED_STRING"

    init {
        Locale.setDefault(Locale.CANADA)
    }

    private fun checkWhetherExceptionIsThrown(
        expression: String,
        expectedException: Exception,
        dataContext: DataContext? = null
    ): Boolean {
        var actualException: Throwable? = null

        logger.errorLoggedCallback = { tag: String?, exception: Throwable? ->
            if (tag == InterpolatorImpl.TAG) {
                actualException = exception
            }
        }

        assert(interpolator.interpolate(expression, dataContext ?: emptyDataContext()) == null)
        return actualException?.message == expectedException.message ?: false
    }

    //region Expressions without interpolation return expected strings.
    @Test
    fun `expression with no interpolation returns original string`() {
        val result = interpolator.interpolate(nonInterpolatedString)
        assert(result == nonInterpolatedString)
    }

    @Test
    fun `expression given Data, UrlParameters and UserInfo and no Interpolation returns original string`() {
        val result = interpolator.interpolate(
            nonInterpolatedString,
            dataContextOf(
                Keyword.DATA.value to mapOf("page" to 2),
                Keyword.URL.value to mapOf("key2" to "value2"),
                Keyword.USER.value to mapOf("userid" to "54321"),
            )
        )
        assert(result == nonInterpolatedString)
    }

    @Test
    fun `expression with incomplete interpolation returns original string`() {
        val expression = "{{user.userid"
        val result = interpolator.interpolate(
            expression,
            dataContextOf(
                Keyword.USER.value to mapOf("userid" to "54321"),
            )
        )
        assert(result == expression)
    }
    //endregion

    //region Basic interpolation exceptions are thrown correctly.
    @Test
    fun `expression with interpolating but no user info throws exception`() {
        assert(checkWhetherExceptionIsThrown("{{user.userid}}", StringExpressionException.UnexpectedValue(value = "user.userid")))
    }

    @Test
    fun `expression with interpolating but no urlParameters throws exception`() {
        assert(checkWhetherExceptionIsThrown("{{url.page}}", StringExpressionException.UnexpectedValue(value = "url.page")))
    }

    @Test
    fun `expression with interpolating but no data throws exception`() {
        assert(checkWhetherExceptionIsThrown("{{data.count}}", StringExpressionException.UnexpectedValue(value = "data.count")))
    }

    @Test
    fun `expression with interpolating but no brackets nor user info throws exception`() {
        interpolator.interpolate("user.userid")
    }
    //endregion

    //region Basic interpolation.
    @Test
    fun `expression with interpolation for userInfo returns expected string`() {
        val result = interpolator.interpolate(
            "{{user.name}}",
            dataContextOf(
                Keyword.USER.value to mapOf("name" to "George")
            )
        )
        assert(result == "George")
    }

    @Test
    fun `expression with nested dataContext returns expected string`() {
        val result = interpolator.interpolate(
            "{{ data.thumbnail.url }}",
            dataContextOf(
                Keyword.DATA.value to mapOf("thumbnail" to mapOf("url" to "www.judo.app"))
            )
        )
        assert(result == "www.judo.app")
    }

    @Test
    fun `expression with excessively nested dataContext returns expected string`() {
        val result = interpolator.interpolate(
            "{{ data.thumbnail.thumbnail.thumbnail.thumbnail.thumbnail.thumbnail.thumbnail.thumbnail.thumbnail.url }}",
            dataContextOf(
                Keyword.DATA.value to mapOf(
                    "thumbnail" to mapOf(
                        "thumbnail" to mapOf(
                            "thumbnail" to mapOf(
                                "thumbnail" to mapOf(
                                    "thumbnail" to mapOf(
                                        "thumbnail" to mapOf(
                                            "thumbnail" to mapOf(
                                                "thumbnail" to mapOf("thumbnail" to mapOf("url" to "www.judo.app"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        assert(result == "www.judo.app")
    }

    @Test
    fun `expression with interpolation for urlParameters returns expected string`() {
        val result = interpolator.interpolate(
            "{{url.page}}",
            dataContextOf(
                Keyword.URL.value to mapOf("page" to "three")
            )
        )
        assert(result == "three")
    }

    @Test
    fun `expression with interpolation for data returns expected string`() {
        val result = interpolator.interpolate(
            "{{data.age}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("age" to "Twenty")
            )
        )
        assert(result == "Twenty")
    }

    @Test
    fun `expression with multiple interpolations returns expected string`() {
        val result = interpolator.interpolate(
            "{{data.page}} {{url.key2}} {{user.userid}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("page" to "2"),
                Keyword.URL.value to mapOf("key2" to "value2"),
                Keyword.USER.value to mapOf("userid" to "54321"),
            )
        )
        assert(result == "2 value2 54321")
    }

    @Test
    fun `expression with different number types returns expected string`() {
        val result = interpolator.interpolate(
            "{{data.int}} {{data.negativeInt}} {{data.double}} {{data.negativeDouble}}",
            dataContextOf(
                Keyword.DATA.value to mapOf(
                    "int" to 2,
                    "negativeInt" to -4,
                    "double" to 2.34,
                    "negativeDouble" to -55.7
                )
            )
        )
        assert(result == "2 -4 2 -56")
    }

    @Test
    fun `expression with html returns expected string`() {
        val result = interpolator.interpolate(
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\"><div style=\"height: 300px\">{{data.body}}</div>",
            dataContextOf(
                Keyword.DATA.value to mapOf("body" to "<div style=\"height: 300px\"><p><b>SAN JOSE</b></div>")
            )
        )
        assert(result == "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\"><div style=\"height: 300px\"><div style=\"height: 300px\"><p><b>SAN JOSE</b></div></div>")
    }

    @Test
    fun `expression with escaped double quotes returns expected string`() {
        val result = interpolator.interpolate(
            "Username: {{user.username}}",
            dataContextOf(
                Keyword.USER.value to mapOf("username" to "\"aperson\"")
            )
        )
        assert(result == "Username: \"aperson\"")
    }
    //endregion

    //region Lowercase helper.
    @Test
    fun `lowercase string returns expected string`() {
        val result = interpolator.interpolate(
            "{{lowercase \"UPPERCASE\"}}"
        )
        assert(result == "uppercase")
    }

    @Test
    fun `lowercase returns expected string`() {
        val result = interpolator.interpolate(
            "{{lowercase data.name}}",
            dataContextOf(Keyword.DATA.value to mapOf("name" to "AN UPPERCASE NAME"))
        )
        assert(result == "an uppercase name")
    }

    @Test
    fun `lowercase invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{lowercase}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "twoArgumentHelper",
                    expected = "2",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `lowercase invalid number of arguments without brackets throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{lowercase}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "twoArgumentHelper",
                    expected = "2",
                    actual = "1"
                )
            )
        )
    }
    //endregion

    //region Uppercase helper.
    @Test
    fun `uppercase string returns expected string`() {
        val result = interpolator.interpolate(
            "{{uppercase \"lowercase\"}}"
        )
        assert(result == "LOWERCASE")
    }

    @Test
    fun `uppercase returns expected string`() {
        val result = interpolator.interpolate(
            "{{uppercase data.name}}",
            dataContextOf(Keyword.DATA.value to mapOf("name" to "a lowercase name"))
        )
        assert(result == "A LOWERCASE NAME")
    }

    @Test
    fun `uppercase invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{uppercase}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "twoArgumentHelper",
                    expected = "2",
                    actual = "1"
                )
            )
        )
    }
    //endregion

    //region Replace helper.
    @Test
    fun `replace string returns expected string`() {
        val result = interpolator.interpolate(
            "{{replace \"lowercased\" \"lower\" \"upper\"}}"
        )
        assert(result == "uppercased")
    }

    @Test
    fun `replace multiple words returns expected string`() {
        val result = interpolator.interpolate(
            "{{replace \"jack be nimble\" \"be nimble\" \"is amazing\"}}"
        )
        assert(result == "jack is amazing")
    }

    @Test
    fun `replace returns expected string`() {
        val result = interpolator.interpolate(
            "{{replace user.message \"should\" \"must\"}}",
            dataContextOf(Keyword.USER.value to mapOf("message" to "You should be good"))
        )
        assert(result == "You must be good")
    }

    @Test
    fun `replace not in string returns initial string`() {
        val result = interpolator.interpolate(
            "{{replace data.name \"M\" \"P\"}}",
            dataContextOf(Keyword.DATA.value to mapOf("name" to "mike"))
        )
        assert(result == "mike")
    }

    @Test
    fun `replace compound expression returns expected string`() {
        val result = interpolator.interpolate(
            "{{ replace (dropLast (dropFirst \"mr. jack reacher\" 4) 8) \"jack\" \"mike\" }}",
        )
        assert(result == "mike")
    }

    @Test
    fun `replace third argument without quote throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace \"lowercased\" lower \"upper\"}}",
                StringExpressionException.InvalidReplaceArguments(
                    argument1 = "lower",
                    argument2 = "\"upper\""
                )
            )
        )
    }

    @Test
    fun `replace fourth argument without quote throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace \"a fox runs\" \"fox\" dog}}",
                StringExpressionException.InvalidReplaceArguments(
                    argument1 = "\"fox\"",
                    argument2 = "dog"
                )
            )
        )
    }

    @Test
    fun `replace third and fourth argument without quote throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace \"a fox runs\" fox dog}}",
                StringExpressionException.InvalidReplaceArguments(
                    argument1 = "fox",
                    argument2 = "dog"
                )
            )
        )
    }

    @Test
    fun `replace invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "replace",
                StringExpressionException.InvalidArgumentNumber(
                    where = "replaceHelper",
                    expected = "4",
                    actual = "1"
                )
            )
        )
    }
    //endregion

    //region dateFormat helper.
    @Test
    fun `dateFormat date string returns expected string`() {
        val result = interpolator.interpolate(
            "{{dateFormat \"2022-02-01 19:46:31+0000\" \"EEEE, d\"}}"
        )
        assert(result == "Tuesday, 1")
    }

    @Test
    fun `dateFormat returns expected string`() {
        val result = interpolator.interpolate(
            "{{dateFormat data.date \"yyyy-MM-dd\"}}, {{dateFormat url.time \"HH:mm:ss\"}}. {{dateFormat user.day \"EEEE, MMM d, yyyy\"}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("date" to "2022-02-01T19:46:31+0000"),
                Keyword.URL.value to mapOf("time" to "2022-02-01 19:46:31+0000"),
                Keyword.USER.value to mapOf("day" to "2022-02-01 19:46:31+0000")
            )
        )
        assert(result == "2022-02-01, 19:46:31. Tuesday, Feb. 1, 2022")
    }

    @Test
    fun `dateFormat test`() {
        val result = interpolator.interpolate(
            "{{date data.datetime \"EEE, MMM d, h:mm a\"}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("datetime" to "2022-02-01T19:46:31")
            )
        )
        assert(result == "Tue., Feb. 1, 7:46 p.m.")
    }

    @Test
    fun `dateFormat test 2`() {
        val result = interpolator.interpolate(
            "{{date data.datetime \"h:mm aa\"}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("datetime" to "2022-02-01T19:46:31")
            )
        )
        assert(result == "7:46 p.m.")
    }

    // Added to cover the legacy use-case of date. This test should be removed once we stop
    // supporting the helper date and move to dateFormat exclusively.
    @Test
    fun `date returns expected string`() {
        val result = interpolator.interpolate(
            "{{date data.date \"yyyy-MM-dd\"}}, {{date url.time \"HH:mm:ss\"}}. {{date user.day \"EEEE, MMM d, yyyy\"}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("date" to "2022-02-01T19:46:31+0000"),
                Keyword.URL.value to mapOf("time" to "2022-02-01 19:46:31+0000"),
                Keyword.USER.value to mapOf("day" to "2022-02-01 19:46:31+0000")
            )
        )
        assert(result == "2022-02-01, 19:46:31. Tuesday, Feb. 1, 2022")
    }

    @Test
    fun `dateFormat invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "dateFormat",
                StringExpressionException.InvalidArgumentNumber(
                    where = "formatDateHelper",
                    expected = "3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `dateFormat invalid date throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dateFormat data.date \"yyyy-MM-dd\"}}",
                StringExpressionException.InvalidDate(
                    argument = "NOTTATDATE!"
                ),
                dataContextOf(
                    Keyword.DATA.value to mapOf("date" to "NOT A DATE!")
                )
            )
        )
    }

    @Test
    fun `dateFormat invalid format throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dateFormat data.date yyyy-MM}}",
                StringExpressionException.InvalidDate(
                    argument = "yyyy-MM"
                ),
                dataContextOf(
                    Keyword.DATA.value to mapOf("date" to "2022-02-01T19:46:31+0000")
                )
            )
        )
    }
    //endregion

    //region dropFirst helper.
    @Test
    fun `dropFirst string returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropFirst \"Boom! Kapow!\" 6}}"
        )
        assert(result == "Kapow!")
    }

    @Test
    fun `dropFirst returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropFirst data.name 4}}",
            dataContextOf(Keyword.DATA.value to mapOf("name" to "Mr. Hulk Hogan"))
        )
        assert(result == "Hulk Hogan")
    }

    @Test
    fun `dropFirst invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "dropFirst",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `dropFirst missing int throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropFirst \"Boom! Kapow!\" shlurp}}",
                StringExpressionException.ExpectedInteger(
                    where = "threeArgumentHelper"
                )
            )
        )
    }
    //endregion

    //region dropFirst helper.
    @Test
    fun `dropLast string returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropLast \"Boom! Kapow!\" 7}}"
        )
        assert(result == "Boom!")
    }

    @Test
    fun `dropLast returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropLast data.alphabet 20}}",
            dataContextOf(Keyword.DATA.value to mapOf("alphabet" to "abcdefghijklmnopqrstuvwxyz"))
        )
        assert(result == "abcdef")
    }

    @Test
    fun `dropLast invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "dropLast",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `dropLast missing int throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropLast \"Boom! Kapow!\" shlurp}}",
                StringExpressionException.ExpectedInteger(
                    where = "threeArgumentHelper"
                )
            )
        )
    }
    //endregion

    //region Prefix helper.
    @Test
    fun `prefix with string returns expected string`() {
        val result = interpolator.interpolate("{{prefix \"Stand by me!\" 8}}",)
        assert(result == "Stand by")
    }

    @Test
    fun `prefix with userInfo returns expected string`() {
        val result = interpolator.interpolate(
            "{{prefix user.title 7}}",
            dataContextOf(
                Keyword.USER.value to mapOf("title" to "Welcome to the jungle")
            )
        )
        assert(result == "Welcome")
    }

    @Test
    fun `prefix with invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{prefix}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `prefix missing int throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{prefix \"Stand by me!\" noIntegers?}}",
                StringExpressionException.ExpectedInteger(
                    where = "threeArgumentHelper"
                )
            )
        )
    }
    //endregion

    //region suffix
    @Test
    fun `suffix with string returns expected string`() {
        val result = interpolator.interpolate("{{suffix \"Boom! Kapow!\" 6}}")
        assert(result == "Kapow!")
    }

    @Test
    fun `suffix with urlParameters returns expected string`() {
        val result = interpolator.interpolate(
            "{{suffix url.alphabet 4}}",
            dataContextOf(
                Keyword.URL.value to mapOf("alphabet" to "abcdefghijklmnopqrstuvwxyz")
            )
        )
        assert(result == "wxyz")
    }

    @Test
    fun `suffix with invalid number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{suffix}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `suffix missing int throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{suffix \"Stand by me!\" noIntegers?}}",
                StringExpressionException.ExpectedInteger(
                    where = "threeArgumentHelper"
                )
            )
        )
    }
    //endregion

    //region Expression nesting.
    @Test
    fun `nested expression with string returns expected string`() {
        val result = interpolator.interpolate("{{ uppercase (suffix (dropFirst \"mr. jack reacher\" 4) 7) }}")
        assert(result == "REACHER")
    }

    @Test
    fun `nested expression with multiple inputs returns expected string`() {
        val result = interpolator.interpolate(
            "{{ lowercase (prefix (dropFirst data.name 4) 3) }} {{uppercase (dropLast data.message 6)}}",
            dataContextOf(
                Keyword.DATA.value to mapOf(
                    "name" to "MR. JONATHON",
                    "message" to "Show me the way to go home!"
                )
            )
        )
        assert(result == "jon SHOW ME THE WAY TO GO")
    }

    @Test
    fun `nested expression with multiple commands returns expected string`() {
        val result = interpolator.interpolate(
            "{{suffix (dropLast (uppercase (dropFirst data.first 5)) 6) 8}}",
            dataContextOf(
                Keyword.DATA.value to mapOf(
                    "first" to "John Smith (Deceased) 1984",
                )
            )
        )
        assert(result == "DECEASED")
    }

    @Test
    fun `nested expression with missing closing parenthesis throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropFirst (uppercase \"morrison\" 5}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "4"
                )
            )
        )
    }

    @Test
    fun `nested expression with missing opening parenthesis throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropFirst uppercase \"morrison\") 5}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "5"
                )
            )
        )
    }
    //endregion

    //region numberFormat helper.
    @Test
    fun `evaluate expression numberFormat string literal non number throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{numberFormat \"Twenty\"}}",
                java.lang.NumberFormatException("For input string: \"Twenty\"")
            )
        )
    }

    @Test
    fun `evaluate expression numberFormat low number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{numberFormat }}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "formatNumberHelper",
                    expected = "2..3",
                    actual = "1"
                )
            )
        )
    }

    @Test
    fun `evaluate expression numberFormat extra number of arguments throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{numberFormat extra extra extra}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "formatNumberHelper",
                    expected = "2..3",
                    actual = "4"
                )
            )
        )
    }

    @Test
    fun `numberFormat with string int returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat user.balance}}",
            dataContextOf(
                Keyword.USER.value to mapOf("balance" to "23")
            )
        )
        assert(result == "23")
    }

    @Test
    fun `numberFormat with string double returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat user.balance}}",
            dataContextOf(
                Keyword.USER.value to mapOf("balance" to "23.55")
            )
        )
        assert(result == "23.55")
    }

    @Test
    fun `numberFormat with string literal int returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"568\"}}",
        )
        assert(result == "568")
    }

    @Test
    fun `numberFormat with string literal double returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"123.456\"}}",
        )
        assert(result == "123.456")
    }

    @Test
    fun `numberFormat with int returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat data.count}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("count" to 30)
            )
        )
        assert(result == "30")
    }

    @Test
    fun `numberFormat with double returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat data.average}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("average" to 12.3487)
            )
        )
        assert(result == "12.349")
    }

    @Test
    fun `evaluate expression with numberFormat none returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\" \"none\"}} {{numberFormat data.number  \"none\"}} {{ numberFormat user.average  \"none\" }}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to "16.8"),
                Keyword.DATA.value to mapOf("number" to 42.5)
            )
        )
        assert(result == "1 42 17")
    }

    @Test
    fun `numberFormat decimal returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\" \"decimal\"}} {{numberFormat data.number \"decimal\"}} {{ numberFormat user.average \"decimal\" }}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to 16.81145),
                Keyword.DATA.value to mapOf("number" to 42.5)
            )
        )
        assert(result == "0.92 42.5 16.811")
    }

    @Test
    fun `numberFormat no style passed returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\"}} {{numberFormat data.number}} {{ numberFormat user.average}}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to 16.81145),
                Keyword.DATA.value to mapOf("number" to 42.5)
            )
        )
        assert(result == "0.92 42.5 16.811")
    }

    @Test
    fun `numberFormat invalid style passed returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\" \"gibberish\"}} {{numberFormat data.number \"gibberish\"}} {{ numberFormat user.average gibberish}}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to 16.81145),
                Keyword.DATA.value to mapOf("number" to 42.5)
            )
        )
        assert(result == "0.92 42.5 16.811")
    }

    @Test
    fun `numberFormat currency returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\" \"currency\"}} {{numberFormat data.number \"currency\"}} {{ numberFormat user.average \"currency\" }}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to 16.81145),
                Keyword.DATA.value to mapOf("number" to 42.5)
            )
        )
        assert(result == "$0.92 $42.50 $16.81")
    }

    @Test
    fun `numberFormat percent returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat \"0.92\" \"percent\"}} {{numberFormat data.number \"percent\"}} {{ numberFormat user.average \"percent\" }}",
            dataContextOf(
                Keyword.USER.value to mapOf("average" to 0.1145),
                Keyword.DATA.value to mapOf("number" to 0.348)
            )
        )
        assert(result == "92% 35% 11%")
    }

    @Test
    fun `numberFormat with nested expressions returns expected string`() {
        val result = interpolator.interpolate(
            "{{numberFormat (dropFirst (dropLast data.amount 5) 3) \"currency\"}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("amount" to "UK£123.45pence")
            )
        )
        assert(result == "$123.45")
    }
    //endregion

    //region Newlines
    @Test
    fun `newLine in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{data.newLine}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("newLine" to "This has a \n in it")
            )
        )
        assert(result == "This has a \n in it")
    }

    @Test
    fun `newLine in string returns expected string`() {
        val result = interpolator.interpolate(
            "{{lowercase \"NEW LINE -> \n <- \"}}",
        )
        assert(result == "new line -> \n <- ")
    }

    @Test
    fun `newLine unicode in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{data.body}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("body" to "Mac inserted new lines \u2028 \u2029")
            )
        )
        assert(result == "Mac inserted new lines \u2028 \u2029")
    }

    @Test
    fun `newLine unicode in string returns expected string`() {
        val result = interpolator.interpolate(
            "{{uppercase \"1st\u20282nd\u20293rd\"}}",
        )
        assert(result == "1ST\u20282ND\u20293RD")
    }

    @Test
    fun `newLine multiple operations in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropFirst (uppercase (replace data.body \"line\" \"sentence\")) 8}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("body" to "This is the first line\nand the second line")
            )
        )
        assert(result == "THE FIRST SENTENCE\nAND THE SECOND SENTENCE")
    }
    //endregion

    //region Quotation marks
    @Test
    fun `replace value mid sentence with smart quotes returns expected string`() {
        val result = interpolator.interpolate(
            "{{replace \"My name is ‟Mike” smith\" \"Mike\" \"JAMES\"}}"
        )
        assert(result == "My name is ‟JAMES” smith")
    }

    @Test
    fun `remove first quotation marks in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropFirst data.name 1}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("name" to "\"Mike\"")
            )
        )
        assert(result == "Mike\"")
    }

    @Test
    fun `uppercase with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{uppercase data.message}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("message" to "Who you going to call? \"Ghostbusters\"")
            )
        )
        assert(result == "WHO YOU GOING TO CALL? \"GHOSTBUSTERS\"")
    }

    @Test
    fun `lowercase with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{lowercase data.phrase}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("phrase" to "I AM \"HE-MAN\"!")
            )
        )
        assert(result == "i am \"he-man\"!")
    }

    @Test
    fun `dropFirst with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropFirst data.phrase 5}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("phrase" to "I AM \"HE-MAN\"!")
            )
        )
        assert(result == "\"HE-MAN\"!")
    }

    @Test
    fun `dropLast with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{dropLast data.message 15}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("message" to "Who you going to call? \"Ghostbusters\"")
            )
        )
        assert(result == "Who you going to call?")
    }

    @Test
    fun `prefix with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{prefix data.sentence 7}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("sentence" to "Ancients spirits of \"evil\"")
            )
        )
        assert(result == "Ancient")
    }

    @Test
    fun `suffix with quotes in data source returns expected string`() {
        val result = interpolator.interpolate(
            "{{suffix data.sentence 6}}",
            dataContextOf(
                Keyword.DATA.value to mapOf("sentence" to "Ancients spirits of \"evil\"")
            )
        )
        assert(result == "\"evil\"")
    }

    @Test
    fun `multiple expressions in string with quotation marks returns expected string`() {
        val result = interpolator.interpolate(
            "{{uppercase data.firstname}} {{lowercase data.lastname}}",
            dataContextOf(
                Keyword.DATA.value to mapOf(
                    "firstname" to "Sally \"Anne\"",
                    "lastname" to "Smith \"(Duck)\""
                )
            )
        )
        assert(result == "SALLY \"ANNE\" smith \"(duck)\"")
    }

    @Test
    fun `replace string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace \"My name is \"Mike\" smith\" \"Mike\" \"JAMES\"}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "replaceHelper",
                    expected = "4",
                    actual = "6"
                )
            )
        )
    }

    @Test
    fun `uppercase string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{uppercase \"Who you going to call? \"Ghostbusters\"!\"}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "twoArgumentHelper",
                    expected = "2",
                    actual = "3"
                )
            )
        )
    }

    @Test
    fun `lowercase string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{lowercase \"Who you going to call? \"Ghostbusters\"!\"}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "twoArgumentHelper",
                    expected = "2",
                    actual = "3"
                )
            )
        )
    }

    @Test
    fun `dropFirst string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropFirst \"My name is \"MIKE\"\" 3}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "4"
                )
            )
        )
    }

    @Test
    fun `dropLast string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropLast \"My name is \"MIKE\"\" 3}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "4"
                )
            )
        )
    }

    @Test
    fun `suffix string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{suffix \"My name is \"MIKE\" Schultz\" 7}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "5"
                )
            )
        )
    }

    @Test
    fun `prefix string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{prefix \"My name is \"MIKE\" Schultz\" 7}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "5"
                )
            )
        )
    }

    @Test
    fun `multiple helpers in string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{ uppercase (dropLast (dropFirst \"mr. \"Jack\" reacher\" 4) 8) }}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "5"
                )
            )
        )
    }

    @Test
    fun `replace in string literal with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace \"Welcome to the \"jungle\".\" \"\"\" \"::\"}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "replaceHelper",
                    expected = "4",
                    actual = "6"
                )
            )
        )
    }

    @Test
    fun `replace in data source with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{replace data.message \"\"\" \"::\"}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "replaceHelper",
                    expected = "4",
                    actual = "5"
                ),
                dataContextOf(
                    Keyword.DATA.value to mapOf("message" to "Welcome to the \"jungle\".")
                )
            )
        )
    }

    @Test
    fun `multiple helpers in data source user with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{ uppercase (dropLast (dropFirst user.fullname 4) 8) }}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "5"
                ),
                dataContextOf(
                    Keyword.USER.value to mapOf("fullname" to "mr. \"Jack\" reacher")
                )
            )
        )
    }

    @Test
    fun `multiple helpers in data source with quotation marks throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropLast (replace (uppercase data.name) \"MIKE\" \"JAMES\") 6}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "replaceHelper",
                    expected = "4",
                    actual = "6"
                ),
                dataContextOf(
                    Keyword.DATA.value to mapOf("name" to "\"Mike\" Jones")
                )
            )
        )
    }

    @Test
    fun `remove surrounding quotation marks in data source throws exception`() {
        assert(
            checkWhetherExceptionIsThrown(
                "{{dropLast (dropFirst data.name 1) 1}} {{dropFirst (dropLast data.name 1) 1}}",
                StringExpressionException.InvalidArgumentNumber(
                    where = "threeArgumentHelper",
                    expected = "3",
                    actual = "4"
                ),
                dataContextOf(
                    Keyword.DATA.value to mapOf("name" to "\"Mike\"")
                )
            )
        )
    }
    //endregion
}
