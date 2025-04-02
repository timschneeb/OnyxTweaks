package me.timschneeberger.onyxtweaks.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.amrdeveloper.codeview.CodeView
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kyuubiran.ezxhelper.Log
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.ActivityTextEditorBinding
import me.timschneeberger.onyxtweaks.databinding.DialogEditorSearchReplaceBinding
import me.timschneeberger.onyxtweaks.ui.editor.plugin.SourcePositionListener
import me.timschneeberger.onyxtweaks.ui.editor.plugin.UndoRedoManager
import me.timschneeberger.onyxtweaks.ui.editor.widget.SymbolInputView
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils
import me.timschneeberger.onyxtweaks.ui.utils.showInputAlert
import me.timschneeberger.onyxtweaks.ui.utils.showYesNoAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences
import java.io.File
import java.util.Locale
import java.util.regex.Pattern


class TextEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextEditorBinding

    private lateinit var codeView: CodeView
    private lateinit var undoRedoManager: UndoRedoManager

    private val prefs by lazy { Preferences(this, PreferenceGroups.TEXT_EDITOR) }

    private var handle = ""
    private var key = "<unknown>"
    private var path = ""
    private var mode = MMKVUtils.EditorMode.PLAIN_TEXT

    private var isDirty = false
        set(value) {
            field = value
            updateName()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        configCodeView()
        configCodeViewPlugins()

        val extraTarget = intent.getStringExtra(EXTRA_TARGET_FILE)
        val extraHandle = intent.getStringExtra(EXTRA_HANDLE)
        if(extraTarget == null || extraHandle == null) {
            finish()
            return
        }

        path = extraTarget
        handle = extraHandle
        intent.getStringExtra(EXTRA_KEY)?.let { key = it }
        intent.getStringExtra(EXTRA_MODE)?.let {
            mode = MMKVUtils.EditorMode.valueOf(it)
        }
        load()

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(isDirty) {
                    this@TextEditorActivity.showYesNoAlert(
                        R.string.editor_save_prompt_title,
                        R.string.editor_save_prompt,
                        R.string.editor_save,
                        R.string.editor_discard
                    ) {
                        if(it) {
                            saveAndClose(false)
                        } else {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                        return@showYesNoAlert
                    }
                }
                else {
                    finish()
                }
            }
        })
    }

    private fun setOkResult() {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_TARGET_FILE, path)
            putExtra(EXTRA_MODE, mode.name)
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_HANDLE, handle)
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        isDirty = savedInstanceState?.getBoolean(STATE_IS_DIRTY) ?: isDirty
        undoRedoManager.clearHistory()

        super.onPostCreate(savedInstanceState)
    }

    private fun load() {
        val content = try {
            File(path).readText()
        } catch(e: Exception) {
            Log.e(e, "Failed to read file: $path")
            toast(getString(R.string.editor_open_fail))
            finish()
            return
        }

        updateName()

        if (mode == MMKVUtils.EditorMode.JSON) {
            try {
                Log.d("Parsing JSON content")
                val mapper = ObjectMapper()
                val json = mapper.readValue(content, Object::class.java)
                codeView.setText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json))
            } catch (e: Exception) {
                Log.e(e, "Failed to parse JSON content")
                codeView.setText(content)
            }
        }
        else if (mode == MMKVUtils.EditorMode.LIST) {
            toast(getString(R.string.editor_string_list_hint))
            codeView.setText(content)
        }
        else {
            codeView.setText(content)
        }

        codeView.reHighlightSyntax()
        undoRedoManager.clearHistory()
        isDirty = false
    }

    private fun saveAndClose(force: Boolean) {
        if(!save(force))
            return
        setOkResult()
        finish()
    }

    private fun save(force: Boolean): Boolean {
        try {
            if (mode == MMKVUtils.EditorMode.JSON) {
                val formatted = try {
                    val mapper = ObjectMapper()
                    val json = mapper.readValue(codeView.text.toString(), Object::class.java)
                    val compact = mapper.writeValueAsString(json)
                    compact
                }
                catch (e: Exception) {
                    if (!force) {
                        showYesNoAlert(
                            getString(R.string.editor_invalid_json),
                            getString(R.string.editor_invalid_json_message, e.message),
                            getString(R.string.save),
                            getString(R.string.cancel)
                        ) {
                            if (it) {
                                saveAndClose(true)
                            }
                        }
                        return false
                    }
                    codeView.text.toString()
                }

                File(path).writeText(formatted)
            } else {
                File(path).writeText(codeView.text.toString())
            }
            isDirty = false
        } catch(e: Exception) {
            Log.e(e, "Failed to write file: $path")
            toast(getString(R.string.editor_open_fail))
            finish()
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun updateName() {
        binding.fileNameText.text = key + if(isDirty) "*" else ""
    }

    private fun configCodeView() {
        binding.codeViewScroller.isSmoothScrollingEnabled = true

        codeView = binding.codeView

        // Change default font to JetBrains Mono font
        val jetBrainsMono = ResourcesCompat.getFont(this, R.font.jetbrainsmono)
        codeView.typeface = jetBrainsMono
        codeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, prefs.get(R.string.key_editor_font_size))

        // Add input view
        binding.symbolInput.bindEditor(codeView)
        binding.symbolInput.addSymbols(
            arrayOf("TAB", "(", ")", "{", "}", "[", "]", ",", ".", ";", "?", ":", "+", "-", "*", "/", "@", "\""),
            arrayOf("\t",  "(", ")", "{", "}", ",", ".", ";", "?", ":", "+", "-", "*", "/", "@", "\"")
        )
        binding.symbolInput.forEachButton(object: SymbolInputView.ButtonConsumer {
            override fun accept(btn: Button) {
                btn.typeface = jetBrainsMono
            }
        })

        // Setup Line number feature
        codeView.setEnableLineNumber(true)
        codeView.setLineNumberTextColor(Color.GRAY)
        codeView.setLineNumberTextSize(25f)

        // Setup highlighting current line
        codeView.setEnableHighlightCurrentLine(true)
        codeView.setHighlightCurrentLineColor(ContextCompat.getColor(this, R.color.colorInverseOnSurface))

        // Setup Auto indenting feature
        codeView.setEnableAutoIndentation(mode == MMKVUtils.EditorMode.JSON)
        codeView.setIndentationStarts(setOf('[', '{'))
        codeView.setIndentationEnds(setOf(']', '}'))
        codeView.setTabLength(4)
        codeView.setTabWidth(4)

        codeView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                isDirty = true
            }
            override fun afterTextChanged(editable: Editable) {}
        })

        // Setup auto pair complete
        val pairCompleteMap: MutableMap<Char, Char> = HashMap()
        pairCompleteMap['{'] = '}'
        pairCompleteMap['['] = ']'
        pairCompleteMap['('] = ')'
        pairCompleteMap['"'] = '"'
        pairCompleteMap['\''] = '\''
        codeView.setPairCompleteMap(pairCompleteMap)
        codeView.enablePairComplete(true)
        codeView.enablePairCompleteCenterCursor(false)

        codeView.setTextColor(getColor(android.R.color.black))
        codeView.setBackgroundColor(getColor(android.R.color.white))
    }

    private fun configCodeViewPlugins() {
        undoRedoManager = UndoRedoManager(codeView)
        undoRedoManager.connect()

        binding.sourcePositionTxt.text = getString(R.string.editor_source_position, 0, 0)
        val sourcePositionListener = SourcePositionListener(codeView)
        sourcePositionListener.setOnPositionChanged { line, column ->
            binding.sourcePositionTxt.text = getString(R.string.editor_source_position, line, column)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putBoolean(STATE_IS_DIRTY, isDirty)
        })
    }

    @Suppress("UsePropertyAccessSyntax")
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        if (menu is MenuBuilder)
            menu.setOptionalIconsVisible(true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.findMenu -> launchEditorButtonSheet()
            R.id.text_undo -> {
                if(undoRedoManager.canUndo())
                    undoRedoManager.undo()
                else
                    toast(getString(R.string.editor_cannot_undo))
            }
            R.id.text_redo -> {
                if(undoRedoManager.canRedo())
                    undoRedoManager.redo()
                else
                    toast(getString(R.string.editor_cannot_redo))
            }
            R.id.text_save -> {
                saveAndClose(false)
            }
            R.id.fontSize -> changeFontSize()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeFontSize() {
        showInputAlert(
            LayoutInflater.from(this),
            R.string.editor_text_size,
            R.string.editor_text_size,
            "%.1f".format(Locale.ROOT, prefs.get<Float>(R.string.key_editor_font_size)),
            true,
            "dp"
        ) {
            it ?: return@showInputAlert
            try {
                val value = it.toFloat()
                if(value < 1)
                    throw NumberFormatException()

                codeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, value)
                prefs.set(R.string.key_editor_font_size, value)
            }
            catch (ex: Exception) {
                Log.e("Failed to parse number input")
                Log.d(ex)
                toast(R.string.error_invalid_number_format, false)
            }
        }
    }

    private fun launchEditorButtonSheet() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_editor_search_replace)
        dialog.window!!.setDimAmount(0f)

        val editor = DialogEditorSearchReplaceBinding.inflate(layoutInflater, null, false)
        dialog.setContentView(editor.root)

        val searchEdit = editor.searchEdit
        val replacementEdit = editor.replacementEdit
        val findPrevAction = editor.findPrevAction
        val findNextAction = editor.findNextAction
        val replacementAction = editor.replaceAction
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                val text = editable.toString().trim { it <= ' ' }
                if (text.isEmpty()) codeView.clearMatches()
                codeView.findMatches(Pattern.quote(text))
            }
        })
        findPrevAction.setOnClickListener { codeView.findPrevMatch() }
        findNextAction.setOnClickListener { codeView.findNextMatch() }
        replacementAction.setOnClickListener {
            val regex = searchEdit.text.toString()
            val replacement = replacementEdit.text.toString()
            codeView.replaceAllMatches(regex, replacement)
        }
        dialog.setOnDismissListener { codeView.clearMatches() }
        dialog.show()
    }

    companion object {
        const val EXTRA_TARGET_FILE = "targetFile"
        const val EXTRA_MODE = "editorMode"
        const val EXTRA_KEY = "name"
        const val EXTRA_HANDLE = "handle"

        const val STATE_IS_DIRTY = "isDirty"

        fun createIntent(context: Context, mode: MMKVUtils.EditorMode, handle: String, key: String, content: String): Intent {
            return Intent(context, TextEditorActivity::class.java).apply {
                val outputFile = File.createTempFile("editor_${key.take(5)}", null, context.cacheDir)
                outputFile.writeText(content)
                putExtra(EXTRA_TARGET_FILE, outputFile.absolutePath)
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_KEY, key)
                putExtra(EXTRA_HANDLE, handle)
            }
        }
    }
}
