package com.comparo.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PlatformPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val urls: Array<String>
) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = mutableMapOf<Int, PlatformFragment>()

    override fun getItemCount(): Int = urls.size

    override fun createFragment(position: Int): Fragment {
        val fragment = PlatformFragment.newInstance(urls[position])
        fragments[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): PlatformFragment? {
        return fragments[position]
    }
}
