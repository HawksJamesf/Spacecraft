package com.hawksjamesf.simpleweather.bean

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawskjamesf
 * @since: Sep/25/2018  Tue
 */
data class Main(
        var temp: Float,//temperature
        var pressure: Int,
        var humidity: Int,//湿度
        var temp_min: Float,
        var temp_max: Float
) {
}