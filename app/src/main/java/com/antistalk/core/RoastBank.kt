package com.antistalk.core

/**
 * Roast message bank L1..L4 + Repeat Attempts. Vietnamese default, English fallback.
 * Never insult appearance / dignity / gender / health — humor, wisdom & self-respect only.
 * {name} and {count} are interpolated at display time.
 */
object RoastBank {
    data class Roast(val level: Int, val vi: String, val en: String, val isRepeatOnly: Boolean = false)

    /**
     * Kịch bản đặc biệt khi người dùng vừa bấm "THÔI, TÔI ĐI RA" xong lại quay lại tìm tiếp trong vòng vài phút.
     */
    private val REPEAT_ROASTS = listOf(
        Roast(1, "Ủa alo? Vừa nãy hứa 'Tôi đi ra' rồi mà? Sao lại mò vào tìm {name} tiếp rồi?!", "Excuse me? You just promised 'I\\'m out'! Why are you searching {name} again?!", true),
        Roast(1, "Ý chí 'thôi tôi đi ra' của bạn kéo dài được có mấy chục giây vậy thôi hả? 😂", "Did your 'I\\'m out' willpower only last 20 seconds? 😂", true),
        Roast(1, "Bắt quả tang nhé! Vừa bấm đi ra xong lại ngựa quen đường cũ vào tìm {name} tiếp!", "Caught red-handed! Pressed leave, then turned right back around!", true),
        Roast(1, "Lại quay xe à? Nãy vừa thề thốt thoát ra, giờ tay lại tự giác gõ tên {name}!", "U-turn already? Swore to exit, now your fingers are typing {name} again!", true),
        Roast(1, "Biết ngay mà! Đã bảo là không kìm lòng được đâu. Lần này đi ra thật đi bạn!", "Knew it! Couldn\\'t resist, huh? Make it a real exit this time!", true),
        Roast(1, "Này, đừng để sự tò mò điều khiển bạn chứ! Vừa mới tắt đi xong mà!", "Hey, don\\'t let curiosity control you! You just closed this a moment ago!", true),
    )

    private val STANDARD_ROASTS = listOf(
        // ─── Level 1: Nhẹ nhàng, nhắc khéo, thức tỉnh ─────────────────────────────
        Roast(1, "Ủa, ngón tay lại tự giác gõ tên {name} nữa rồi kìa.", "Your fingers typed {name}'s name on autopilot again."),
        Roast(1, "Tò mò một chút thì vui, tò mò nhiều chút là tụt mood cả ngày nha.", "Curiosity is cute until it ruins your mood all day."),
        Roast(1, "Story người ta không có bạn đâu, vào xem chỉ tổ làm giàu cho view thôi.", "You're not in their story. You're just boosting their views."),
        Roast(1, "Dừng lại 3 giây. Hít sâu một hơi. Có thật sự cần xem {name} không?", "Pause for 3 seconds. Deep breath. Do you actually need to see {name}?"),
        Roast(1, "Lướt qua thôi, đừng dừng lại. Bạn xứng đáng bận rộn với những điều tốt đẹp hơn.", "Keep scrolling. Your time is worth so much better."),
        Roast(1, "Thôi nào, người ta đang bận sống cuộc đời của họ, bạn cũng sống cuộc đời của bạn đi.", "They're living their life. Go live yours."),
        Roast(1, "Không có gì mới đâu bạn ơi, kiểm tra làm gì cho nặng lòng.", "Nothing new there, friend. Why burden your heart?"),
        Roast(1, "Bạn biết mình sắp làm gì, và bạn cũng biết cảm giác sau đó sẽ thế nào mà.", "You know what you're doing, and how you'll feel right after."),
        Roast(1, "Hôm nay đã làm được gì cho bản thân chưa mà lại đi quan tâm {name} rồi?", "Done anything for yourself today before checking on {name}?"),
        Roast(1, "Người ta không tìm bạn, cớ sao bạn cứ phải tìm {name}?", "They aren't searching for you. Why are you searching for {name}?"),

        // ─── Level 2: Cà khịa bạn thân, hài hước, châm biếm nhẹ ───────────────────
        Roast(2, "Lại stalk nữa à bạn thân? Bộ làm thanh tra mạng xã hội có được trả lương không?", "Stalking again, bestie? Does the social media FBI pay you?"),
        Roast(2, "Tìm {name} làm gì? Tính xem người ta đang vui vẻ hơn bạn đến mức nào hả?", "Looking up {name} to check how much happier they are without you?"),
        Roast(2, "Người ta đi ngủ rồi, hoặc đang đi chơi rồi. Chỉ có bạn là còn thức canh profile thôi.", "They're asleep or out having fun. Only you are guarding their profile."),
        Roast(2, "Bạn đang tìm thông tin mới, hay đang tự tìm một lý do để đêm nay mất ngủ?", "Looking for new updates, or a reason to stay awake crying?"),
        Roast(2, "Điện thoại mua mấy chục triệu để làm giàu và học tập, không phải để theo dõi {name} đâu nha.", "Your expensive phone is for success, not for tracking {name}."),
        Roast(2, "FBI còn không chăm chỉ điểm danh profile {name} bằng bạn nữa đó.", "Even the FBI doesn't check {name}'s profile this religiously."),
        Roast(2, "Có những thứ càng tìm càng đau. Tắt app đi bật bài nhạc chill nghe xem nào.", "The more you dig, the more it hurts. Put on some chill music instead."),
        Roast(2, "Xem xong rồi lại thở dài, xong lại tự trách mình. Kịch bản này diễn hoài không chán hả?", "Look, sigh, regret. Aren't you tired of this exact script?"),
        Roast(2, "Nếu sự kiên trì này đổi thành tiền thì bạn đã mua được căn nhà rồi đó.", "If this persistence was currency, you'd own a penthouse by now."),
        Roast(2, "Profile người ta chứ có phải trang điểm danh đâu mà ngày nào cũng ghé?", "Their profile isn't a punch card. Why are you clocking in daily?"),
        Roast(2, "Bạn tính theo dõi người ta đến khi nào? Đến khi {name} cưới luôn hả?", "Planning to follow them until their wedding day or what?"),
        Roast(2, "Người ta không có một dòng trạng thái nào về bạn đâu, đừng tìm trong vô vọng nữa.", "Not a single status is about you. Stop reading between non-existent lines."),

        // ─── Level 3: Gắt hơn, đánh thẳng vào tâm lý 'nghiện stalk' ──────────────
        Roast(3, "Lần thứ {count} hôm nay rồi đấy! Định đăng ký làm fan cứng trọn đời à?", "That's #{count} today! Applying to be their lifetime fan?"),
        Roast(3, "Đừng tự lừa mình là 'chỉ vô tình'. Bạn vừa gõ từng chữ tên {name} vào ô tìm kiếm mà!", "Don't pretend it was an accident. You typed every letter of {name}'s name!"),
        Roast(3, "Người ta quên bạn lâu rồi, chỉ có bạn là còn chăm chỉ làm 'nhà nghiên cứu học' thôi.", "They moved on ages ago. You're the only one doing archaeological research."),
        Roast(3, "{name} mà biết bạn vào xem nhiều thế này chắc cũng phải ngại giùm bạn luôn á.", "If {name} knew how often you check, they'd cringe on your behalf."),
        Roast(3, "Tự trọng bay màu đâu hết rồi? Quay xe gấp đi bạn ơi!", "Where did your self-respect go? U-turn right now!"),
        Roast(3, "Nhìn lại số lần bấm xem hôm nay ({count} lần) xem có thấy xót cho thời gian của mình không?", "#{count} checks today. Don't you feel sorry for your own precious time?"),
        Roast(3, "Họ không nhắn tin nghĩa là họ không muốn nói chuyện. Đừng vào check đèn xanh đèn đỏ nữa.", "No text means no interest. Stop checking if their active dot is green."),
        Roast(3, "Thế giới 8 tỉ người, tại sao ngày nào cũng dán mắt vào đúng cái tài khoản này?", "8 billion people on Earth, why glue your eyes to this exact account?"),
        Roast(3, "Người ta up ảnh đẹp là để thiên hạ ngắm, không phải để bạn ngồi soi từng cọng tóc đâu.", "They post photos for everyone, not for you to overanalyze every pixel."),
        Roast(3, "Bớt tò mò về cuộc sống của {name} lại. Cuộc đời bạn đang cần bạn chăm sóc hơn kìa.", "Stop caring about {name}'s life. Your own life needs you much more."),

        // ─── Level 4: Tỉnh ngộ cực mạnh, đánh thức lòng tự trọng ─────────────────
        Roast(4, "Dừng lại đi bạn ơi. Tự thương lấy bản thân mình một chút được không?", "Stop it. Please love and respect yourself a little more."),
        Roast(4, "3 giây can đảm: thoát ra, uống ngụm nước, cất điện thoại. Đừng tự làm tổn thương mình nữa.", "3 seconds of courage: exit, drink water, put down the phone. Stop hurting yourself."),
        Roast(4, "Bạn có nhận ra mỗi lần vào xem trang của {name}, bạn đều đánh mất một phần tự tin không?", "Do you realize every time you look, you lose a piece of your confidence?"),
        Roast(4, "Người ta chẳng bận tâm bạn đang làm gì đâu. Đừng biến mình thành cái bóng vô hình nữa.", "They don't wonder what you're doing. Stop being a ghost in their shadow."),
        Roast(4, "Hôm nay là lần thứ {count} rồi. Buông điện thoại xuống, đi rửa mặt và làm việc có ích đi.", "#{count} times today. Put the phone down, wash your face, and do something meaningful."),
        Roast(4, "Bạn tìm {name} hay đang tự kiếm cớ để xát muối vào tim mình vậy?", "Are you checking on {name}, or just rubbing salt into your own wounds?"),
        Roast(4, "Người ta tiến về phía trước rồi, sao bạn cứ thích đứng lại nhìn về quá khứ mãi thế?", "They're moving forward. Why are you still standing there staring at the past?"),
        Roast(4, "Tắt màn hình đi. Đi ngủ sớm hoặc kiếm gì ăn đi. Bạn xứng đáng được trân trọng hơn thế này.", "Turn off the screen. Sleep early or grab food. You deserve so much better than this.")
    )

    val ALL = STANDARD_ROASTS + REPEAT_ROASTS

    @Volatile
    private var lastPickedRaw: String = ""

    /**
     * Chọn ngẫu nhiên một câu cà khịa phù hợp với cấp độ và ngữ cảnh,
     * đảm bảo KHÔNG BAO GIỜ bị lặp lại câu vừa hiển thị trước đó.
     */
    fun pick(level: Int, countToday: Int, name: String, vi: Boolean, isRepeatAttempt: Boolean = false): String {
        val candidates = if (isRepeatAttempt) {
            REPEAT_ROASTS
        } else {
            STANDARD_ROASTS.filter { it.level <= level }.ifEmpty { STANDARD_ROASTS }
        }
        val pool = candidates.filter { it.vi != lastPickedRaw }.ifEmpty { candidates }
        val chosen = pool.random()
        lastPickedRaw = chosen.vi
        val raw = if (vi) chosen.vi else chosen.en
        return raw.replace("{count}", countToday.toString()).replace("{name}", name)
    }
}
