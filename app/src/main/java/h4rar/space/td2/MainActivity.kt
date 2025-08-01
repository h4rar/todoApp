package h4rar.space.td2

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var repository: NotesRepository
    lateinit var db: NotesDatabase
    private lateinit var adapter: TabsAdapter
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var isLongPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            NotesDatabase::class.java, "notes_database"
        ).build()
        repository = NotesRepository(db.tabDao(), db.noteDao())

        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val addTabButton = findViewById<android.widget.ImageButton>(R.id.addTabButton)
        val addNoteButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.addNoteButton)

        adapter = TabsAdapter(this, repository)
        viewPager.adapter = adapter

        // Настройка кастомных вкладок
        setupTabLayoutMediator(tabLayout, viewPager)

        // Обновление вкладок при изменении данных
        CoroutineScope(Dispatchers.Main).launch {
            repository.allTabs.collect { tabs ->
                adapter.updateTabs(tabs)
                // Очищаем ViewPager, если вкладок нет
                if (tabs.isEmpty()) {
                    viewPager.adapter = adapter // Переустанавливаем адаптер для очистки
                    tabLayoutMediator?.detach() // Отсоединяем старый медиатор
                    setupTabLayoutMediator(tabLayout, viewPager) // Пересоздаём медиатор
                } else {
                    // Пересоздаём TabLayoutMediator для синхронизации
                    tabLayoutMediator?.detach()
                    setupTabLayoutMediator(tabLayout, viewPager)
                }
                updateTabIcons()
                updateTabStyles()
            }
        }

        // Слушатель выбора вкладок для изменения стилей
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateTabStyles()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabStyles()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Обработчик нажатия на кнопку добавления вкладки
        addTabButton.setOnClickListener {
            showAddTabDialog()
        }

        // Обработчик нажатия на кнопку добавления заметки
        addNoteButton.setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (adapter.itemCount > 0) {
                val currentTab = adapter.getTabAt(currentPosition)
                showAddNoteDialog(currentTab.id)
            } else {
                // Если вкладок нет, можно показать сообщение
                MaterialAlertDialogBuilder(this)
                    .setTitle("No Tabs")
                    .setMessage("Please create a tab first.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        // Обработчик нажатия кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                var handled = false
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                    updateTabStyles()
                    handled = true
                }
                // Сбрасываем режим долгого нажатия для заметок
                val currentTabPosition = viewPager.currentItem
                adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
                if (!handled) {
                    // Стандартное поведение
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupTabLayoutMediator(tabLayout: TabLayout, viewPager: androidx.viewpager2.widget.ViewPager2) {
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null)
            val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
            val editTabIcon = tabView.findViewById<ImageView>(R.id.editTabIcon)
            val deleteTabIcon = tabView.findViewById<ImageView>(R.id.deleteTabIcon)

            tabTitle.text = adapter.getTabName(position)
            editTabIcon.visibility = if (isLongPressed) View.VISIBLE else View.GONE
            deleteTabIcon.visibility = if (isLongPressed) View.VISIBLE else View.GONE

            tab.customView = tabView
        }.also { it.attach() }
    }

    private fun updateTabStyles() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val tabView = tab?.customView
            val tabTitle = tabView?.findViewById<TextView>(R.id.tabTitle)

            if (tab?.isSelected == true) {
                tabTitle?.textSize = 26f
                tabTitle?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                tabTitle?.textSize = 16f
                tabTitle?.setTextColor(ContextCompat.getColor(this, R.color.light_gray))
            }
        }
    }

    private fun updateTabIcons() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val tabView = tab?.customView
            val tabTitle = tabView?.findViewById<TextView>(R.id.tabTitle)
            val editTabIcon = tabView?.findViewById<ImageView>(R.id.editTabIcon)
            val deleteTabIcon = tabView?.findViewById<ImageView>(R.id.deleteTabIcon)

            editTabIcon?.visibility = if (isLongPressed) View.VISIBLE else View.GONE
            deleteTabIcon?.visibility = if (isLongPressed) View.VISIBLE else View.GONE

            tabView?.setOnLongClickListener {
                isLongPressed = !isLongPressed
                updateTabIcons()
                true
            }

            tabView?.setOnClickListener {
                val position = tab?.position ?: return@setOnClickListener
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                    updateTabStyles()
                }
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }

            deleteTabIcon?.setOnClickListener {
                val position = tab?.position ?: return@setOnClickListener
                if (position >= 0) {
                    val tabData = adapter.getTabAt(position)
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Удалить вкладку")
                        .setMessage("Вы уверены, что хотите удалить вкладку \"${tabData.name}\"? Все заметки в этой вкладке также будут удалены.")
                        .setPositiveButton("OK") { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.deleteTab(tabData)
                                runOnUiThread {
                                    isLongPressed = false
                                    updateTabIcons()
                                    updateTabStyles()
                                    // Переключаемся на предыдущую вкладку, если удалена текущая
                                    val newPosition = if (position > 0) position - 1 else 0
                                    if (adapter.itemCount > 0) {
                                        viewPager.setCurrentItem(newPosition, true)
                                    }
                                }
                            }
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }

            editTabIcon?.setOnClickListener {
                val position = tab?.position ?: return@setOnClickListener
                if (position >= 0) {
                    showEditTabDialog(adapter.getTabAt(position))
                }
            }
        }
    }

    private fun showAddTabDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tab, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.tabNameInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("New Tab")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val tabName = editText.text.toString().trim()
                if (tabName.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.insertTab(Tab(name = tabName))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTabDialog(tab: Tab) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tab, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.tabNameInput)
        editText.setText(tab.name)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Tab")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateTab(tab.copy(name = newName))
                        runOnUiThread {
                            isLongPressed = false
                            updateTabIcons()
                            updateTabStyles()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddNoteDialog(tabId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.noteInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("New Note")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val noteText = editText.text.toString().trim()
                if (noteText.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.insertNote(Note(text = noteText, tabId = tabId))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tabLayoutMediator?.detach()
    }
}