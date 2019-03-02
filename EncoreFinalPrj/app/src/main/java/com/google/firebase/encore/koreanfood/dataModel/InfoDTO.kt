package com.google.firebase.encore.koreanfood.dataModel


data class InfoDTO (var name_k: String? = null, /* 한국 음식 이름 */
                    var name_j: String? = null, /* 일본 음식 이름 */
                    var name_e: String? = null, /* 영어 음식 이름 */
                    var name_c: String? = null, /* 중국 음식 이름 */
                    var info_j: String? = null, /* 일본 음식 정보 */
                    var info_e: String? = null, /* 영어 음식 정보 */
                    var info_c: String? = null, /* 중국 음식 정보 */
                    var a_t: String? = null, /* 견과류 알레르기 */
                    var a_s: String? = null, /* 어폐류 알레르기 */
                    var a_m: String? = null, /* 우유 알레르기 */
                    var a_e: String? = null /* 달걀 알레르기 */
)