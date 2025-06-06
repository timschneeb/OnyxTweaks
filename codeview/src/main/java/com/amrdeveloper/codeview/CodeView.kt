/*
 * MIT License
 *
 * Copyright (c) 2020 AmrDeveloper (Amr Hesham)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.amrdeveloper.codeview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * CodeView is a CustomView to provide a lot of features that you need to highlights
 * and creating an editor for your custom programming language or data format
 */
class CodeView : AppCompatMultiAutoCompleteTextView, Findable, Replaceable {
    private var tabWidth = 0
    private var tabLength = 0
    private var tabWidthInCharacters = 0
    private var mUpdateDelayTime = 500
    private var modified = true
    private var highlightWhileTextChanging = true

    // Line number options
    private var lineNumberRect = Rect()
    private lateinit var lineNumberPaint: Paint
    private var enableLineNumber = false
    private var enableRelativeLineNumber = false

    // Highlighting current line options
    private var lineBounds = Rect()
    private lateinit var highlightLinePaint: Paint
    private var enableHighlightCurrentLine = false

    // Indentations options
    private var currentIndentation = 0
    private var enableAutoIndentation = false
    private val indentationStarts: MutableSet<Char> = HashSet()
    private val indentationEnds: MutableSet<Char> = HashSet()

    // Matches and tokens
    private var currentMatchedIndex = -1
    private var matchingColor = Color.YELLOW
    private var currentMatchedToken: CharacterStyle? = null
    private val matchedTokens: MutableList<Token> = ArrayList()

    // Auto pair complete
    private var enablePairComplete = false
    private var enablePairCompleteCenterCursor = false
    private val mPairCompleteMap: MutableMap<Char, Char> = HashMap()
    private val mUpdateHandler = Handler(Looper.getMainLooper())
    private val mSyntaxPatternMap: MutableMap<Pattern, Int> = LinkedHashMap()

    constructor(context: Context) : super(context) {
        initEditorView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        initEditorView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initEditorView()
    }

    private fun initEditorView() {
        setHorizontallyScrolling(false)
        addTextChangedListener(mEditorTextWatcher)
        setOnKeyListener(mOnKeyListener)
        lineNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        lineNumberPaint.style = Paint.Style.FILL
        lineBounds = Rect()
        highlightLinePaint = Paint()
        highlightLinePaint.color = LINE_HIGHLIGHT_DEFAULT_COLOR
    }

    override fun onDraw(canvas: Canvas) {
        if (enableLineNumber || enableHighlightCurrentLine) {
            val fullText = text
            val layout = layout
            val lineCount = lineCount
            val selectionStart = Selection.getSelectionStart(fullText)
            val cursorLine = layout.getLineForOffset(selectionStart)

            // Highlight the current line with custom color by drawing rectangle on this line
            if (enableHighlightCurrentLine) {
                getLineBounds(cursorLine, lineBounds)
                canvas.drawRect(lineBounds, highlightLinePaint)
            }

            // Draw line number or relative line number
            if (enableLineNumber) {
                for (i in 0 until lineCount) {
                    val baseline = getLineBounds(i, null)
                    if (i == 0 || fullText[layout.getLineStart(i) - 1] == '\n') {
                        // If relative line number is enabled the number should be the absolute value of cursorLine - i)
                        // if not it should be just current line number
                        // Add 1 to the current line number to make it start from 1 not 0
                        val lineNumber =
                            if (i == cursorLine || !enableRelativeLineNumber) i + 1 else abs(
                                cursorLine - i
                            )
                        canvas.drawText(
                            " $lineNumber",
                            lineNumberRect.left.toFloat(),
                            baseline.toFloat(),
                            lineNumberPaint
                        )
                    }
                }

                // Calculate padding depending on current line number
                val paddingLeft = 50 + log10(lineCount.toDouble()).toInt() * 10
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }
        super.onDraw(canvas)
    }

    override fun findMatches(regex: String): List<Token> {
        matchedTokens.clear()
        if (regex.isEmpty()) return matchedTokens
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        while (matcher.find()) matchedTokens.add(Token(matcher.start(), matcher.end()))
        return matchedTokens
    }

    override fun findNextMatch(): Token? {
        if (matchedTokens.isEmpty()) return null
        currentMatchedIndex++
        if (currentMatchedIndex >= matchedTokens.size) currentMatchedIndex = 0
        val currentMatch = matchedTokens[currentMatchedIndex]
        clearHighlightingMatchingToken()
        highlightMatchingToken(currentMatch)
        return currentMatch
    }

    override fun findPrevMatch(): Token? {
        if (matchedTokens.isEmpty()) return null
        currentMatchedIndex--
        if (currentMatchedIndex < 0) currentMatchedIndex = 0
        val currentMatch = matchedTokens[currentMatchedIndex]
        clearHighlightingMatchingToken()
        highlightMatchingToken(currentMatch)
        return currentMatch
    }

    override fun clearMatches() {
        clearHighlightingMatchingToken()
        currentMatchedToken = null
        currentMatchedIndex = -1
        matchedTokens.clear()
    }

    override fun replaceFirstMatch(regex: String?, replacement: String?) {
        regex ?: return
        replacement ?: return

        val pattern = Pattern.compile(regex)
        val text = pattern.matcher(text.toString()).replaceFirst(replacement)
        setTextHighlighted(text)
    }

    override fun replaceAllMatches(regex: String?, replacement: String?) {
        regex ?: return
        replacement ?: return

        val pattern = Pattern.compile(regex)
        val text = pattern.matcher(text.toString()).replaceAll(replacement)
        setTextHighlighted(text)
    }

    private fun highlightSyntax(editable: Editable) {
        if (mSyntaxPatternMap.isEmpty()) return
        val syntaxSet: Set<Map.Entry<Pattern, Int>> = mSyntaxPatternMap.entries
        for ((key, value) in syntaxSet) {
            val matcher = key.matcher(editable)
            while (matcher.find()) {
                createForegroundColorSpan(editable, matcher, value)
            }
        }
    }

    private fun createForegroundColorSpan(
        editable: Editable,
        matcher: Matcher,
        @ColorInt color: Int
    ) {
        editable.setSpan(
            ForegroundColorSpan(color),
            matcher.start(), matcher.end(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun createBackgroundColorSpan(
        editable: Editable,
        matcher: Matcher,
        @ColorInt color: Int
    ) {
        editable.setSpan(
            BackgroundColorSpan(color),
            matcher.start(), matcher.end(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun highlightMatchingToken(token: Token) {
        currentMatchedToken = BackgroundColorSpan(matchingColor)
        editableText.setSpan(
            currentMatchedToken,
            token.start, token.end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun clearHighlightingMatchingToken() {
        if (currentMatchedToken == null) return
        editableText.removeSpan(currentMatchedToken)
    }

    private fun highlight(editable: Editable): Editable {
        if (editable.isEmpty()) return editable
        try {
            clearSpans(editable)
            highlightSyntax(editable)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        return editable
    }

    private fun highlightWithoutChange(editable: Editable) {
        modified = false
        highlight(editable)
        modified = true
    }

    /**
     * Replace the current text with new highlighted text
     * @param text The new Text
     */
    fun setTextHighlighted(text: CharSequence?) {
        if (text.isNullOrEmpty()) return
        cancelHighlighterRender()
        modified = false
        setText(highlight(SpannableStringBuilder(text)))
        modified = true
    }

    /**
     * Modify the tab length to use it in auto indenting feature
     * @param length The new tab length value
     */
    fun setTabLength(length: Int) {
        tabLength = length
    }

    /**
     * Modify the current tab with
     * @param characters to use it to calculate the tab width
     */
    fun setTabWidth(characters: Int) {
        if (tabWidthInCharacters == characters) return
        tabWidthInCharacters = characters
        tabWidth = (paint.measureText("m") * characters).roundToInt()
    }

    private fun clearSpans(editable: Editable) {
        val length = editable.length
        val foregroundSpans = editable.getSpans(
            0, length, ForegroundColorSpan::class.java
        )
        run {
            var i = foregroundSpans.size
            while (i-- > 0) {
                editable.removeSpan(foregroundSpans[i])
            }
        }
        val backgroundSpans = editable.getSpans(
            0, length, BackgroundColorSpan::class.java
        )
        var i = backgroundSpans.size
        while (i-- > 0) {
            editable.removeSpan(backgroundSpans[i])
        }
    }

    /**
     * Stop the highlighter task
     */
    fun cancelHighlighterRender() {
        mUpdateHandler.removeCallbacks(mUpdateRunnable)
    }

    private fun convertTabs(editable: Editable, start: Int, count: Int) {
        var current = start
        if (tabWidth < 1) return
        val s = editable.toString()
        val stop = current + count
        while (s.indexOf("\t", current).also { current = it } > -1 && current < stop) {
            editable.setSpan(
                TabWidthSpan(),
                current,
                current + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ++current
        }
    }

    /**
     * Un highlight all keywords by removing all spans
     */
    fun resetHighlighter() {
        clearSpans(text)
    }

    /**
     * Enable or disable auto indenting feature
     * @param enableAutoIndentation Flag to enable or disable auto indenting
     */
    fun setEnableAutoIndentation(enableAutoIndentation: Boolean) {
        this.enableAutoIndentation = enableAutoIndentation
    }

    /**
     * Set the indenting starts set of characters
     * @param characters Set of characters to use them as indenting starts
     * @since 1.2.1
     */
    fun setIndentationStarts(characters: Set<Char>?) {
        indentationStarts.clear()
        indentationStarts.addAll(characters ?: setOf())
    }

    /**
     * Set the indenting ends set of characters
     * @param characters Set of characters to use them as indenting ends
     * @since 1.2.1
     */
    fun setIndentationEnds(characters: Set<Char>?) {
        indentationEnds.clear()
        indentationEnds.addAll(characters ?: setOf())
    }

    /**
     * Re Highlight the syntax patterns
     */
    fun reHighlightSyntax() {
        highlightSyntax(editableText)
    }

    /**
     * Enable or disable the line number feature
     * @param enableLineNumber Flag to enable or disable line number
     * @since 1.1.0
     */
    fun setEnableLineNumber(enableLineNumber: Boolean) {
        this.enableLineNumber = enableLineNumber
    }

    /**
     * @return `true` if the line number is enabled
     * @since 1.1.0
     */
    fun isLineNumberEnabled(): Boolean {
        return enableLineNumber
    }

    /**
     * Enable or disable the highlighting current line feature
     * @param enableHighlightCurrentLine  Flag to enable or disable highlighting current line
     * @since 1.3.6
     */
    fun setEnableHighlightCurrentLine(enableHighlightCurrentLine: Boolean) {
        this.enableHighlightCurrentLine = enableHighlightCurrentLine
    }

    /**
     * @return (@code true) if highlighting current line feature is enabled
     * @since 1.3.6
     */
    fun isHighlightCurrentLineEnabled(): Boolean {
        return enableHighlightCurrentLine
    }

    /**
     * Modify the highlight current line  color
     * @param color The new color value
     * @since 1.1.0
     */
    fun setHighlightCurrentLineColor(color: Int) {
        highlightLinePaint.color = color
    }

    /**
     * Modify the line number text color
     * @param color The new color value
     * @since 1.1.0
     */
    fun setLineNumberTextColor(color: Int) {
        lineNumberPaint.color = color
    }

    /**
     * Modify the line number text size
     * @param size The new size value
     * @since 1.1.0
     */
    fun setLineNumberTextSize(size: Float) {
        lineNumberPaint.textSize = size
    }

    /**
     * Modify the matches tokens highlighting color
     * @param color The new color value
     * @since 1.2.1
     */
    fun setMatchingHighlightColor(color: Int) {
        matchingColor = color
    }

    /**
     * Modify the typeface of line number
     * @param typeface The typeface to be set
     * @since 1.3.4
     */
    fun setLineNumberTypeface(typeface: Typeface?) {
        lineNumberPaint.typeface = typeface
    }

    /**
     * Enable or disable the auto pairs complete feature
     * @param enable Flag to enable or disable auto pair complete
     * @since 1.3.0
     */
    fun enablePairComplete(enable: Boolean) {
        enablePairComplete = enable
    }

    /**
     * Enable or disable moving the cursor to the center after insert pair complete
     * @param enable Flag to enable or disable pair complete center cursor
     * @since 1.3.4
     */
    fun enablePairCompleteCenterCursor(enable: Boolean) {
        enablePairCompleteCenterCursor = enable
    }

    /**
     * Set the pairs for auto pairs complete feature
     * @param map Map of pairs of characters
     * @since 1.3.0
     */
    fun setPairCompleteMap(map: Map<Char, Char>?) {
        mPairCompleteMap.clear()
        mPairCompleteMap.putAll(map!!)
    }

    /**
     * Add new pair complete item using key and value
     * @param key the pair complete item key
     * @param value the pair complete item value
     * @since 1.3.0
     */
    fun addPairCompleteItem(key: Char, value: Char) {
        mPairCompleteMap[key] = value
    }

    /**
     * Remove single pair complete item by key
     * @param key the pair complete item key
     * @since 1.3.0
     */
    fun removePairCompleteItem(key: Char) {
        mPairCompleteMap.remove(key)
    }

    /**
     * Clear all of pairs
     * @since 1.3.0
     */
    fun clearPairCompleteMap() {
        mPairCompleteMap.clear()
    }

    private val mUpdateRunnable = Runnable {
        val source = text
        highlightWithoutChange(source)
    }
    private val mOnKeyListener = OnKeyListener { _, keyCode, _ ->
        if (!enableAutoIndentation) return@OnKeyListener false
        when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> currentIndentation++
            KeyEvent.KEYCODE_DEL -> if (currentIndentation > 0) currentIndentation--
        }
        false
    }
    private val mEditorTextWatcher: TextWatcher = object : TextWatcher {
        private var start = 0
        private var count = 0
        override fun beforeTextChanged(
            charSequence: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            this.start = start
            this.count = count
        }

        override fun onTextChanged(
            charSequence: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            if (!modified) return
            if (highlightWhileTextChanging) {
                if (mSyntaxPatternMap.isNotEmpty()) {
                    convertTabs(editableText, start, count)
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime.toLong())
                }
            }
            if (count == 1 && (enableAutoIndentation || enablePairComplete)) {
                val currentChar = charSequence[start]
                if (enableAutoIndentation) {
                    if (indentationStarts.contains(currentChar)) currentIndentation += tabLength else if (indentationEnds.contains(
                            currentChar
                        )
                    ) currentIndentation -= tabLength
                }
                if (enablePairComplete) {
                    val pairValue = mPairCompleteMap[currentChar]
                    if (pairValue != null) {
                        modified = false
                        val selectionEnd = selectionEnd
                        text.insert(selectionEnd, pairValue.toString())
                        if (enablePairCompleteCenterCursor) setSelection(selectionEnd)
                        if (enableAutoIndentation) {
                            if (indentationStarts.contains(pairValue)) currentIndentation += tabLength else if (indentationEnds.contains(
                                    pairValue
                                )
                            ) currentIndentation -= tabLength
                        }
                        modified = true
                    }
                }
            }
        }

        override fun afterTextChanged(editable: Editable) {
            if (!highlightWhileTextChanging) {
                if (!modified) return
                cancelHighlighterRender()
                if (mSyntaxPatternMap.isNotEmpty()) {
                    convertTabs(editableText, start, count)
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime.toLong())
                }
            }
        }
    }

    private inner class TabWidthSpan : ReplacementSpan() {
        override fun getSize(
            paint: Paint, text: CharSequence,
            start: Int, end: Int, fm: FontMetricsInt?
        ): Int {
            return tabWidth
        }

        override fun draw(
            canvas: Canvas, text: CharSequence,
            start: Int, end: Int, x: Float,
            top: Int, y: Int, bottom: Int, paint: Paint
        ) {
        }
    }

    private val mInputFilter = InputFilter { source, start, _, dest, dStart, dEnd ->
        if (modified && enableAutoIndentation && start < source.length) {
            if (source[start] == '\n') {
                // Apply the current indentation if it inserted at the end
                if (dest.length == dEnd) return@InputFilter applyIndentation(
                    source,
                    currentIndentation
                )

                // reCalculate the current indentation
                var indentation = calculateSourceIndentation(dest.subSequence(0, dStart))

                // Decrement the indentation if the next char is on indentationEnds set
                if (indentationEnds.contains(dest[dEnd])) indentation -= tabLength

                // Apply the new indentation to the source code
                return@InputFilter applyIndentation(source, indentation)
            }
        }
        source
    }

    private fun applyIndentation(source: CharSequence, indentation: Int): CharSequence {
        val sourceCode = StringBuilder()
        sourceCode.append(source)
        for (i in 0 until indentation) sourceCode.append(" ")
        return sourceCode.toString()
    }

    private fun calculateSourceIndentation(source: CharSequence): Int {
        var indentation = 0
        val lines =
            source.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            indentation += calculateExtraIndentation(line)
        }
        return indentation
    }

    private fun calculateExtraIndentation(line: String): Int {
        if (line.isEmpty()) return 0
        val firstChar = line[line.length - 1]
        if (indentationStarts.contains(firstChar)) return tabLength else if (indentationEnds.contains(
                firstChar
            )
        ) return -tabLength
        return 0
    }

    companion object {
        private const val LINE_HIGHLIGHT_DEFAULT_COLOR = Color.DKGRAY
        private val PATTERN_LINE = Pattern.compile("(^(.*)$)+", Pattern.MULTILINE)
    }
}