package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dcrandroid.adapter.AccountAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Account;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import mobilewallet.LibWallet;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class AccountsFragment extends Fragment {

    private List<Account> accounts = new ArrayList<>();
    private AccountAdapter accountAdapter;

    private PreferenceUtil util;

    private LibWallet wallet;

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.content_account, container, false);

        recyclerView = vi.findViewById(R.id.recycler_view2);

        return vi;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK){
            String accountName = data.getStringExtra(Constants.ACCOUNT_NAME);
            int accountNumber = data.getIntExtra(Constants.ACCOUNT_NUMBER, -1);
            for (int i = 0; i < accounts.size(); i++){
                if (accounts.get(i).getAccountNumber() == accountNumber){
                    accounts.get(i).setAccountName(accountName);
                    accountAdapter.notifyItemChanged(i);
                    return;
                }
            }
        }
    }

    public void prepareAccountData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    accounts.clear();
                    accounts.addAll(Account.parse(wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)));
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            accountAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() == null || getContext() == null) {
            return;
        }
        getActivity().setTitle(getString(R.string.account));
        util = new PreferenceUtil(getActivity());

        MainActivity.menuOpen.setVisible(true);
        accountAdapter = new AccountAdapter(accounts, getContext());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.gray_divider));
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(accountAdapter);
        registerForContextMenu(recyclerView);

        wallet = DcrConstants.getInstance().wallet;

        prepareAccountData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity.menuOpen.setVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext() != null){
            getContext().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getContext() != null){
            IntentFilter filter = new IntentFilter(Constants.SYNCED);
            getContext().registerReceiver(receiver, filter);
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(Constants.SYNCED)) {
                prepareAccountData();
            }
        }
    };
}