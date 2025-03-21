package me.timschneeberger.onyxtweaks.ui.editor.syntax

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.amrdeveloper.codeview.CodeView
import me.timschneeberger.onyxtweaks.R
import java.util.regex.Pattern

class JsonLanguage(private val context: Context, private val codeView: CodeView) {

    //Language Keywords
    private val PATTERN_BUILTINS = Pattern.compile("[,;\\[\\]{}()]")
    private val PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*")
    private val PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")
    private val PATTERN_FUNCTION = Pattern.compile("\\b\\w+(?=\\([^\\n]*\\))")
    private val PATTERN_FUNCTION_SIGNATURE = Pattern.compile("(?<=function)\\s+[^\\s\\(]+")
    private val PATTERN_OPERATION = Pattern.compile("\\*|=|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|\\*|\\||/|/=")
    private val PATTERN_CONDITION = Pattern.compile("\\?|:")
    private val PATTERN_ANNOTATION = Pattern.compile("(?<=\\n)[^\\S@\\n]*@.[a-zA-Z0-9]+")
    private val PATTERN_NUMBERS = Pattern.compile("\\b(-?\\d+[.]?\\d*f?)\\b")
    private val PATTERN_CHAR = Pattern.compile("['](.*?)[']")
    private val PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]")
    private val PATTERN_HEX = Pattern.compile("0x[0-9a-fA-F]+")

    private val PATTERN_PROPERTY = Pattern.compile("(?<=^|\\n)\\s*((?:[A-Za-z0-9])+:)")
    private val PATTERN_PROPERTY_RIGHT = Pattern.compile("(?<=^|\\n)\\s*((?:[A-Za-z0-9])+:)[^\\n]*")

    init {
        applyTheme()
    }

    private fun applyTheme() {
        fun col(@ColorRes resId: Int) = ContextCompat.getColor(context, resId)

        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, col(R.color.monokia_pro_purple))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, col(R.color.monokia_pro_purple))
        // codeView.addSyntaxPattern(PATTERN_KEYWORDS, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, col(R.color.monokia_pro_white_dim))
        codeView.addSyntaxPattern(PATTERN_ANNOTATION, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_FUNCTION, col(R.color.monokia_pro_green))
        // codeView.addSyntaxPattern(PATTERN_CONSTANTS, col(R.color.monokia_pro_sky))
        // codeView.addSyntaxPattern(PATTERN_PREFDEF_VARS, col(R.color.monokia_pro_sky))
        codeView.addSyntaxPattern(PATTERN_OPERATION, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_CONDITION, col(R.color.monokia_pro_orange))
        codeView.addSyntaxPattern(PATTERN_FUNCTION_SIGNATURE, col(R.color.monokia_pro_green))

        codeView.addSyntaxPattern(PATTERN_CHAR, col(R.color.monokia_pro_green))
        codeView.addSyntaxPattern(PATTERN_STRING, col(R.color.monokia_pro_orange))

        codeView.addSyntaxPattern(PATTERN_PROPERTY_RIGHT, col(R.color.monokia_pro_sky_dim))
        codeView.addSyntaxPattern(PATTERN_PROPERTY, col(R.color.monokia_pro_sky))

        codeView.addSyntaxPattern(PATTERN_SINGLE_LINE_COMMENT, col(R.color.monokia_pro_grey))
        codeView.addSyntaxPattern(PATTERN_MULTI_LINE_COMMENT, col(R.color.monokia_pro_grey))

        //Default Color
        codeView.reHighlightSyntax()
    }

    val indentationStarts: Set<Char>
        get() {
            val characterSet: MutableSet<Char> = HashSet()
            characterSet.add('(')
            return characterSet
        }
    val indentationEnds: Set<Char>
        get() {
            val characterSet: MutableSet<Char> = HashSet()
            characterSet.add(')')
            return characterSet
        }
}