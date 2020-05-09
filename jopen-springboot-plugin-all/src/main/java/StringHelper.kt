import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.math.NumberUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * @author maxuefeng
 */
object StringHelper {
    /**
     * 去掉所有特殊字符
     *
     * @param origin
     * @return
     */
    fun format(origin: String): String {
        return origin.replace("[`qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……& amp;*（）——+|{}【】‘；：”“’。，、？|-]".toRegex(), "")
    }

    /**
     * @param origin
     * @param excludeChars
     * @return
     */
    fun format(origin: String, excludeChars: Array<Char?>?): String {
        var regex = "[`qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……& amp;*（）——+|{}【】‘；：”“’。，、？|-]"
        // 为空判断否则会出现NullPointException
        if (excludeChars != null && excludeChars.size > 0) {
            for (excludeChar in excludeChars) {
                regex = regex.replace(excludeChar!!, ' ')
            }
        }
        return origin.replace(regex.toRegex(), "")
    }

    fun convert(num: Int) { //12345
        val mark = arrayOf("", "十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千", "万")
        val numCn = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val numArrRev = num.toString().toCharArray()
        val container = StringBuilder()
        for (i in numArrRev.indices) {
            val `val` = Integer.valueOf(numArrRev[i].toString())
            var number = numCn[`val`]
            val x = numArrRev.size - i - 1
            var sign = mark[x]
            if (`val` == 0) {
                if (x % 4 != 0) { // 删除单位
                    sign = ""
                }
                if (i < numArrRev.size - 1) {
                    val val1 = numArrRev[i + 1].toString().toInt()
                    if (`val` == val1) {
                        number = ""
                    } else if ("万" == sign || "亿" == sign) {
                        number = ""
                    }
                } else if (i == numArrRev.size - 1) {
                    number = ""
                }
            }
            container.append(number).append(sign)
        }
        println("$num-->$container")
    }

    /**
     * 大写数字
     */
    private val NUMBERS = arrayOf("零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖")
    /**
     * 整数部分的单位
     */
    private val IUNIT = arrayOf("元", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟", "万", "拾", "佰",
            "仟")
    /**
     * 小数部分的单位
     */
    private val DUNIT = arrayOf("角", "分", "厘")

    /**
     * 得到大写金额。
     */
    fun toChinese(str: String): String {
        var str = str
        str = str.replace(",".toRegex(), "") // 去掉","
        var integerStr: String // 整数部分数字
        val decimalStr: String // 小数部分数字
        // 初始化：分离整数部分和小数部分
        if (str.indexOf(".") > 0) {
            integerStr = str.substring(0, str.indexOf("."))
            decimalStr = str.substring(str.indexOf(".") + 1)
        } else if (str.indexOf(".") == 0) {
            integerStr = ""
            decimalStr = str.substring(1)
        } else {
            integerStr = str
            decimalStr = ""
        }
        // integerStr去掉首0，不必去掉decimalStr的尾0(超出部分舍去)
        if (integerStr != "") {
            integerStr = java.lang.Long.toString(integerStr.toLong())
            if (integerStr == "0") {
                integerStr = ""
            }
        }
        // overflow超出处理能力，直接返回
        if (integerStr.length > IUNIT.size) {
            println("$str:超出处理能力")
            return str
        }
        val integers = toArray(integerStr) // 整数部分数字
        val isMust5 = isMust5(integerStr) // 设置万单位
        val decimals = toArray(decimalStr) // 小数部分数字
        return getChineseInteger(integers, isMust5) + getChineseDecimal(decimals)
    }

    /**
     * 整数部分和小数部分转换为数组，从高位至低位
     */
    private fun toArray(number: String): IntArray {
        val array = IntArray(number.length)
        for (i in 0 until number.length) {
            array[i] = number.substring(i, i + 1).toInt()
        }
        return array
    }

    /**
     * 得到中文金额的整数部分。
     */
    private fun getChineseInteger(integers: IntArray, isMust5: Boolean): String {
        val chineseInteger = StringBuilder("")
        val length = integers.size
        for (i in 0 until length) { // 0出现在关键位置：1234(万)5678(亿)9012(万)3456(元)
// 特殊情况：10(拾元、壹拾元、壹拾万元、拾万元)
            var key = ""
            if (integers[i] == 0) {
                if (length - i == 13) // 万(亿)(必填)
                    key = IUNIT[4] else if (length - i == 9) // 亿(必填)
                    key = IUNIT[8] else if (length - i == 5 && isMust5) // 万(不必填)
                    key = IUNIT[4] else if (length - i == 1) // 元(必填)
                    key = IUNIT[0]
                // 0遇非0时补零，不包含最后一位
                if (length - i > 1 && integers[i + 1] != 0) key += NUMBERS[0]
            }
            chineseInteger.append(if (integers[i] == 0) key else NUMBERS[integers[i]] + IUNIT[length - i - 1])
        }
        return chineseInteger.toString()
    }

    /**
     * 得到中文金额的小数部分。
     */
    private fun getChineseDecimal(decimals: IntArray): String {
        val chineseDecimal = StringBuilder()
        for (i in decimals.indices) { // 舍去3位小数之后的
            if (i == 3) break
            chineseDecimal.append(if (decimals[i] == 0) "" else NUMBERS[decimals[i]] + DUNIT[i])
        }
        return chineseDecimal.toString()
    }

    /**
     * 判断第5位数字的单位"万"是否应加。
     */
    private fun isMust5(integerStr: String): Boolean {
        val length = integerStr.length
        return if (length > 4) {
            val subInteger: String
            subInteger = if (length > 8) { // 取得从低位数，第5到第8位的字串
                integerStr.substring(length - 8, length - 4)
            } else {
                integerStr.substring(0, length - 4)
            }
            subInteger.toInt() > 0
        } else {
            false
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        System.err.println(randomString(20))
        System.err.println(randomString(40))
    }

    //首字母转小写
    fun toLowerCaseFirstOne(s: String): String {
        return if (Character.isLowerCase(s[0])) s else Character.toLowerCase(s[0]).toString() + s.substring(1)
    }

    //首字母转大写
    fun toUpperCaseFirstOne(s: String): String {
        return if (Character.isUpperCase(s[0])) s else Character.toUpperCase(s[0]).toString() + s.substring(1)
    }

    /**
     * 从字符串中解析出一个数字
     *
     * @param origin
     * @return
     */
    fun parseNumber(origin: String): Double {
        val amount = StringBuilder()
        for (i in 0 until origin.length) {
            val c = origin[i]
            val value = c.toString()
            if ("." == value && i != 0) {
                amount.append(".")
                continue
            }
            val digits = NumberUtils.isDigits(value)
            if (!digits && i != 0 && "" != amount.toString()) {
                return NumberUtils.toDouble(amount.toString())
            }
            if (digits) {
                amount.append(value)
            }
        }
        //
        return if ("" == amount.toString()) {
            0.0
        } else NumberUtils.toDouble(amount.toString())
    }

    fun extractUrl(source: String?): String {
        val pattern = Pattern.compile("((http[s]{0,1}|ftp)://[a-zA-Z0-9\\.\\-]+\\.([a-zA-Z]{2,4})(:\\d+)?(/[a-zA-Z0-9\\.\\-~!@#$%^&*+?:_/=<>]*)?)|((www.)|[a-zA-Z0-9\\.\\-]+\\.([a-zA-Z]{2,4})(:\\d+)?(/[a-zA-Z0-9\\.\\-~!@#$%^&*+?:_/=<>]*)?)")
        val matcher = pattern.matcher(source)
        val buffer = StringBuilder()
        while (matcher.find()) {
            buffer.append(matcher.group())
        }
        return buffer.toString()
    }

    fun of(bytes: ByteArray?, charset: Charset?): String {
        return String(bytes!!, charset!!)
    }

    fun of(bytes: ByteArray?): String {
        return String(bytes!!, StandardCharsets.UTF_8)
    }

    fun randomString(length: Int): String {
        val allStr = arrayOf(
                "z", "x", "c", "v", "b", "n", "m", "a", "s", "d", "f", "g", "h", "j", "k", "l", "o", "i", "p", "u", "y", "t", "r", "e", "w", "q",
                "Z", "X", "C", "V", "B", "N", "M", "A", "S", "D", "F", "G", "H", "J", "K", "L", "O", "I", "P", "U", "Y", "T", "R", "E", "W", "Q",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        )
        val returnVal = StringBuilder()
        for (i in 0 until length) {
            returnVal.append(allStr[RandomUtils.nextInt(0, allStr.size)])
        }
        return returnVal.toString()
    }
}