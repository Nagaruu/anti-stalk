package com.antistalk.core

/**
 * Roast message bank L1..L4. Vietnamese default, English fallback.
 * Never insult appearance / dignity / gender / health — humor only.
 * {name} and {count} are interpolated at display time.
 */
object RoastBank {
    data class Roast(val level: Int, val vi: String, val en: String)

    val ALL = listOf(
        Roast(1, "Thôi nào. Không có gì mới đâu.", "Come on. Nothing new there."),
        Roast(1, "Ồ. Lại tìm người ta à?", "Oh. Looking them up again?"),
        Roast(1, "Bạn biết mình đang làm gì mà.", "You know what you're doing."),
        Roast(2, "Lại stalk nữa à, bạn thân?", "Stalking again, bestie?"),
        Roast(2, "Bạn đang tìm thông tin mới, hay đang tìm một lý do để buồn?", "New info, or a new reason to feel sad?"),
        Roast(3, "Lần thứ {count} hôm nay rồi. Định làm tình báo không lương à?", "That's #{count} today. Unpaid intel agent now?"),
        Roast(3, "{name} vẫn sống tốt mà không cần bạn check đâu.", "{name} is doing fine without your check-ins."),
        Roast(4, "Bạn đang tìm người ta hay đang tìm một lý do để tự làm mình buồn?", "Are you looking for them, or for a reason to hurt yourself?"),
        Roast(4, "3 giây can đảm: thoát ra và uống nước. {name} không chạy đi đâu.", "3 seconds of courage: back out and drink water. {name} isn't going anywhere."),
    )

    fun pick(level: Int, countToday: Int, name: String, vi: Boolean): String {
        val pool = ALL.filter { it.level <= level }.ifEmpty { ALL }
        val r = pool[(countToday + name.length) % pool.size]
        val raw = if (vi) r.vi else r.en
        return raw.replace("{count}", countToday.toString()).replace("{name}", name)
    }
}
