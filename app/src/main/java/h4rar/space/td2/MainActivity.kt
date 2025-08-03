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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var repository: NotesRepository
    lateinit var db: NotesDatabase
    private lateinit var adapter: TabsAdapter
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var isLongPressed = false
    private lateinit var easterEggOverlay: android.view.View

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            // Проверяем, что нажатие не на кнопки вкладок
            val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
            val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            
            // Получаем координаты нажатия
            val x = ev.x
            val y = ev.y
            
            // Проверяем, что нажатие не на TabLayout (где находятся кнопки вкладок)
            val tabLayoutLocation = IntArray(2)
            tabLayout.getLocationOnScreen(tabLayoutLocation)
            val tabLayoutLeft = tabLayoutLocation[0]
            val tabLayoutTop = tabLayoutLocation[1]
            val tabLayoutRight = tabLayoutLeft + tabLayout.width
            val tabLayoutBottom = tabLayoutTop + tabLayout.height
            
            // Если нажатие не на TabLayout, скрываем кнопки
            if (x < tabLayoutLeft || x > tabLayoutRight || y < tabLayoutTop || y > tabLayoutBottom) {
                // Проверяем, что нажатие не на кнопки добавления
                val addTabButton = findViewById<android.widget.ImageButton>(R.id.addTabButton)
                val addNoteButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.addNoteButton)
                
                val addTabLocation = IntArray(2)
                addTabButton.getLocationOnScreen(addTabLocation)
                val addTabLeft = addTabLocation[0]
                val addTabTop = addTabLocation[1]
                val addTabRight = addTabLeft + addTabButton.width
                val addTabBottom = addTabTop + addTabButton.height
                
                val addNoteLocation = IntArray(2)
                addNoteButton.getLocationOnScreen(addNoteLocation)
                val addNoteLeft = addNoteLocation[0]
                val addNoteTop = addNoteLocation[1]
                val addNoteRight = addNoteLeft + addNoteButton.width
                val addNoteBottom = addNoteTop + addNoteButton.height
                
                // Если нажатие не на кнопки добавления, скрываем кнопки
                if ((x < addTabLeft || x > addTabRight || y < addTabTop || y > addTabBottom) &&
                    (x < addNoteLeft || x > addNoteRight || y < addNoteTop || y > addNoteBottom)) {
                    // Скрываем кнопки вкладок при нажатии на пустую область
                    if (isLongPressed) {
                        isLongPressed = false
                        updateTabIcons()
                    }
                    // Сбрасываем режим долгого нажатия для заметок
                    val currentTabPosition = viewPager.currentItem
                    adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            NotesDatabase::class.java, "notes_database"
        ).addMigrations(NotesDatabase.MIGRATION_1_2)
         .addCallback(NotesDatabase.DATABASE_CALLBACK)
         .build()
        repository = NotesRepository(db.tabDao(), db.noteDao())
        
        // Инициализируем данные при первом запуске
        initializeDefaultData()

        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val addTabButton = findViewById<android.widget.ImageButton>(R.id.addTabButton)
        val addNoteButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.addNoteButton)
        easterEggOverlay = findViewById(R.id.easterEggOverlay)

        adapter = TabsAdapter(this, repository)
        viewPager.adapter = adapter

        // Обработчик перелистывания ViewPager для скрытия кнопок
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Скрываем кнопки при перелистывании вкладок
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                }
                // Сбрасываем режим долгого нажатия для заметок
                adapter.getFragmentAt(position)?.resetLongPressMode()
            }
        })

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
                // Скрываем кнопки при переключении вкладок
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                }
                // Сбрасываем режим долгого нажатия для заметок
                val currentTabPosition = viewPager.currentItem
                adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabStyles()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Обработчик нажатия на кнопку добавления вкладки
        addTabButton.setOnClickListener {
            // Скрываем кнопки при нажатии на кнопку добавления вкладки
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
            showAddTabDialog()
        }

        // Обработчик нажатия на кнопку добавления заметки
        addNoteButton.setOnClickListener {
            // Скрываем кнопки при нажатии на кнопку добавления заметки
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
            
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

        // Обработчик нажатия на основную область экрана для скрытия кнопок
        findViewById<View>(android.R.id.content).setOnClickListener {
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
        }

        // Обработчик нажатия на корневой layout для скрытия кнопок
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainLayout).setOnClickListener {
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
        }

        // Обработчик нажатия на ViewPager2 для скрытия кнопок
        viewPager.setOnClickListener {
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
        }

        // Обработчик нажатия на пустую область ViewPager2
        viewPager.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                }
                // Сбрасываем режим долгого нажатия для заметок
                val currentTabPosition = viewPager.currentItem
                adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
            }
            false // Позволяем событию продолжить обработку
        }

        // Обработчик нажатия на TabLayout для скрытия кнопок
        tabLayout.setOnClickListener {
            if (isLongPressed) {
                isLongPressed = false
                updateTabIcons()
            }
            // Сбрасываем режим долгого нажатия для заметок
            val currentTabPosition = viewPager.currentItem
            adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
        }

        // TouchListener на корневой layout для скрытия кнопок
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainLayout).setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (isLongPressed) {
                    isLongPressed = false
                    updateTabIcons()
                }
                // Сбрасываем режим долгого нажатия для заметок
                val currentTabPosition = viewPager.currentItem
                adapter.getFragmentAt(currentTabPosition)?.resetLongPressMode()
            }
            false // Позволяем событию продолжить обработку
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
                        .setTitle("Delete Tab")
                        .setMessage("Are you sure you want to delete the \"${tabData.name}\" tab? All notes in this tab will also be deleted.")
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
            .setTitle("New tab")
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
            .setTitle("New task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val noteText = editText.text.toString().trim()
                if (noteText.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Получаем количество заметок в текущей вкладке для определения позиции
                        val notes = repository.getNotesByTab(tabId).first()
                        val newPosition = notes.size
                        repository.insertNote(Note(text = noteText, tabId = tabId, position = newPosition))
                        
                        // Проверяем на пасхалку
                        if (noteText == "09.11.2024") {
                            runOnUiThread {
                                showEasterEgg()
                            }
                        }
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
    
    private fun initializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Проверяем, есть ли уже вкладки в базе данных
            val existingTabs = repository.allTabs.first()
            
            // Если вкладок нет, создаем вкладки по умолчанию
            if (existingTabs.isEmpty()) {
                val homeTab = Tab(name = "Home")
                val workTab = Tab(name = "Work")
                
                repository.insertTab(homeTab)
                repository.insertTab(workTab)
            }
        }
    }
    
    private fun showEasterEgg() {
        // Показываем пасхалку
        easterEggOverlay.visibility = android.view.View.VISIBLE
        
        // Скрываем через 3 секунды
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            easterEggOverlay.visibility = android.view.View.GONE
        }
    }
}