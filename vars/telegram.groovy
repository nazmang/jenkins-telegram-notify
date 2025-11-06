// vars/telegram.groovy

/**
 * Sends a message to Telegram.
 * @param botToken Telegram bot token
 * @param chatId Telegram chat ID
 * @param message Message text
 * @param parseMode Formatting mode (by default MarkdownV2)
 */
def sendMessage(String botToken, String chatId, String message, String parseMode = 'MarkdownV2') {
    try {
        def escapedMessage
        if (parseMode == 'MarkdownV2') {
            escapedMessage = escapeMarkdownV2(message)
        } else if (parseMode == 'HTML') {
            escapedMessage = escapeHTML(message)
        } else {
            escapedMessage = message    
        }
        def url = "https://api.telegram.org/bot${botToken}/sendMessage"
        def response = sh(script: """
            curl -s -X POST '${url}' \
            -d 'chat_id=${chatId}' \
            -d 'text=${escapedMessage}' \
            -d 'parse_mode=${parseMode}'
        """, returnStdout: true).trim()
        echo "Telegram response: ${response}"
    } catch (Exception e) {
        echo "Failed to send Telegram message: ${e.message}"
    }
}
/** 
 * Escapes special characters for MarkdownV2
 * @param text Source text
 * @return Escaped text
 */
private String escapeMarkdownV2(String text) {
    def specialChars = ['_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!']
    def escapedText = text
    specialChars.each { ch ->
        escapedText = escapedText.replace(ch, "\\${ch}")
    }
    return escapedText
}

/** 
 * Escapes special characters for HTML
 * @param text Source text
 * @return Escaped text
 */
private String escapeHTML(String text) {
    return text
        .replace('&', '&amp;')
        .replace('<', '&lt;')
        .replace('>', '&gt;')
}