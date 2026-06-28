package com.zenith.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zenith.app.R;

/**
 * Temporary placeholder fragment used for modules not yet implemented.
 * Will be replaced module by module as features are built.
 */
public class PlaceholderFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_placeholder, container, false);
        String label = getArguments() != null ?
                getArguments().getString("label", "Coming Soon") : "Coming Soon";
        TextView tv = root.findViewById(R.id.tvPlaceholder);
        tv.setText(label + "\n\nThis feature is coming soon!");
        return root;
    }
}
