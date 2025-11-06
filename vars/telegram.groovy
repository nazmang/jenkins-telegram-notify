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
 * Only escapes characters that are NOT part of valid MarkdownV2 formatting patterns
 * @param text Source text
 * @return Escaped text
 */
private String escapeMarkdownV2(String text) {
    // Characters that need escaping in MarkdownV2 (but not if part of formatting)
    // We'll use a regex-based approach to preserve formatting patterns
    
    // First, protect formatting patterns by replacing them with placeholders
    def patterns = [:]
    def placeholderIndex = 0
    
    // Protect bold: *text*
    text = text.replaceAll(/\*([^*]+)\*/) { match, content ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "*${escapeMarkdownV2Content(content)}*"
        placeholderIndex++
        return placeholder
    }
    
    // Protect italic: _text_ (but not __text__ which is not valid MarkdownV2)
    text = text.replaceAll(/(?<!_)_([^_\n]+)_(?!_)/) { match, content ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "_${escapeMarkdownV2Content(content)}_"
        placeholderIndex++
        return placeholder
    }
    
    // Protect strikethrough: ~text~
    text = text.replaceAll(/~([^~]+)~/) { match, content ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "~${escapeMarkdownV2Content(content)}~"
        placeholderIndex++
        return placeholder
    }
    
    // Protect spoiler: ||text||
    text = text.replaceAll(/\|\|([^|]+)\|\|/) { match, content ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "||${escapeMarkdownV2Content(content)}||"
        placeholderIndex++
        return placeholder
    }
    
    // Protect inline code: `text`
    text = text.replaceAll(/`([^`]+)`/) { match, content ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "`${escapeMarkdownV2Content(content)}`"
        placeholderIndex++
        return placeholder
    }
    
    // Protect links: [text](url)
    text = text.replaceAll(/\[([^\]]+)\]\(([^)]+)\)/) { match, linkText, url ->
        def placeholder = "___PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "[${escapeMarkdownV2Content(linkText)}](${escapeMarkdownV2Content(url)})"
        placeholderIndex++
        return placeholder
    }
    
    // Now escape all remaining special characters
    def specialChars = ['_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!']
    specialChars.each { ch ->
        text = text.replace(ch, "\\${ch}")
    }
    
    // Restore protected patterns
    patterns.each { placeholder, value ->
        text = text.replace(placeholder, value)
    }
    
    return text
}

/**
 * Escapes special characters in content (inside formatting tags)
 * @param text Content text
 * @return Escaped text
 */
private String escapeMarkdownV2Content(String text) {
    def specialChars = ['_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!']
    def escapedText = text
    specialChars.each { ch ->
        escapedText = escapedText.replace(ch, "\\${ch}")
    }
    return escapedText
}

/** 
 * Escapes special characters for HTML
 * Preserves valid HTML tags while escaping content
 * @param text Source text
 * @return Escaped text
 */
private String escapeHTML(String text) {
    // First, protect valid HTML tags by replacing them with placeholders
    def patterns = [:]
    def placeholderIndex = 0
    
    // Protect complete HTML tags with content: <tag>content</tag>
    // Match any valid Telegram HTML tag: b, strong, i, em, u, ins, s, strike, del, code, pre, a
    // Use non-greedy matching to handle multiple tags
    // Match each tag type separately to ensure opening and closing tags match
    def tagTypes = ['b', 'strong', 'i', 'em', 'u', 'ins', 's', 'strike', 'del', 'code', 'pre']
    tagTypes.each { tag ->
        text = text.replaceAll(/<${tag}>([\s\S]*?)<\/${tag}>/) { match, content ->
            def placeholder = "___HTML_PLACEHOLDER_${placeholderIndex}___"
            patterns[placeholder] = "<${tag}>${escapeHTMLContent(content)}</${tag}>"
            placeholderIndex++
            return placeholder
        }
    }
    // Handle anchor tags separately (they have attributes)
    text = text.replaceAll(/<a\s+[^>]*>([\s\S]*?)<\/a>/) { match, content ->
        def placeholder = "___HTML_PLACEHOLDER_${placeholderIndex}___"
        // Extract the opening tag from the full match
        def openTagMatch = (match =~ /<a\s+[^>]*>/)
        def openTag = openTagMatch ? openTagMatch[0] : '<a>'
        patterns[placeholder] = "${openTag}${escapeHTMLContent(content)}</a>"
        placeholderIndex++
        return placeholder
    }
    
    // Protect self-closing tags: <tag/>
    text = text.replaceAll(/<(b|strong|i|em|u|ins|s|strike|del|code|pre|a\s+[^>]+)\s*\/>/) { match, tag ->
        def placeholder = "___HTML_PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "<${tag}/>"
        placeholderIndex++
        return placeholder
    }
    
    // Protect standalone opening tags: <tag>
    text = text.replaceAll(/<(b|strong|i|em|u|ins|s|strike|del|code|pre|a\s+[^>]+)>/) { match, tag ->
        def placeholder = "___HTML_PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "<${tag}>"
        placeholderIndex++
        return placeholder
    }
    
    // Protect standalone closing tags: </tag>
    text = text.replaceAll(/<\/(b|strong|i|em|u|ins|s|strike|del|code|pre|a)>/) { match, tag ->
        def placeholder = "___HTML_PLACEHOLDER_${placeholderIndex}___"
        patterns[placeholder] = "</${tag}>"
        placeholderIndex++
        return placeholder
    }
    
    // Now escape all remaining special characters
    // Escape & first (but not if already part of an entity)
    text = text.replaceAll(/&(?!amp;|lt;|gt;|quot;|#\d+;|#x[0-9a-fA-F]+;)/, '&amp;')
    // Escape < and > that are not part of protected tags
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    
    // Restore protected patterns
    patterns.each { placeholder, value ->
        text = text.replace(placeholder, value)
    }
    
    return text
}

/**
 * Escapes special characters in HTML content (inside tags)
 * @param text Content text
 * @return Escaped text
 */
private String escapeHTMLContent(String text) {
    // Escape & first (but not if already part of an entity)
    text = text.replaceAll(/&(?!amp;|lt;|gt;|quot;|#\d+;|#x[0-9a-fA-F]+;)/, '&amp;')
    // Escape < and > in content (but preserve HTML tags if nested)
    // For nested tags, we need to escape them too
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    return text
}