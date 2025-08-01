package h4rar.space.td2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabsAdapter(
    fragmentActivity: FragmentActivity,
    private val repository: NotesRepository
) : FragmentStateAdapter(fragmentActivity) {
    private var tabs: List<Tab> = emptyList()
    private val fragmentManager = fragmentActivity.supportFragmentManager

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return NotesFragment.newInstance(tabs[position].id)
    }

    override fun getItemId(position: Int): Long {
        // Используем уникальный ID вкладки для предотвращения кэширования
        return tabs[position].id.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        // Проверяем, существует ли вкладка с данным ID
        return tabs.any { it.id.toLong() == itemId }
    }

    fun updateTabs(newTabs: List<Tab>) {
        tabs = newTabs
        notifyDataSetChanged()
    }

    fun getTabName(position: Int): String {
        return tabs.getOrNull(position)?.name ?: ""
    }

    fun getTabAt(position: Int): Tab {
        return tabs[position]
    }

    fun getFragmentAt(position: Int): NotesFragment? {
        return fragmentManager.findFragmentByTag("f$position") as? NotesFragment
    }
}