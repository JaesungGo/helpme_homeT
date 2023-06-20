package com.example.mocap01;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class Adapter_Squat extends FragmentStateAdapter {

    public int mCount;

    public Adapter_Squat(FragmentActivity fa, int count) {
        super(fa);
        mCount = count;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int index1 = getRealPosition(position);

        if (index1 == 0) return new Fragment_Squat.Fragment_s1();
        else if (index1 == 1) return new Fragment_Squat.Fragment_s2();
        else if (index1 == 2) return new Fragment_Squat.Fragment_s3();
        else return null; // 예외 처리: 유효하지 않은 index에 대해서는 null을 반환하거나 적절한 처리를 수행하세요.
    }


    @Override
    public int getItemCount() {
        return 2000;
    }

    public int getRealPosition(int position) { return position % mCount; }


}
