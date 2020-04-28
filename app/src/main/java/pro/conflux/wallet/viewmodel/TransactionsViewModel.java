package pro.conflux.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;


import pro.conflux.wallet.domain.CfxWallet;
import pro.conflux.wallet.entity.NetworkInfo;
import pro.conflux.wallet.entity.Transaction;
import pro.conflux.wallet.interact.FetchTransactionsInteract;
import pro.conflux.wallet.interact.FetchWalletInteract;
import pro.conflux.wallet.repository.CfxNetworkRepository;
import pro.conflux.wallet.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class TransactionsViewModel extends BaseViewModel {
    private static final long FETCH_TRANSACTIONS_INTERVAL = 1;
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<CfxWallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final CfxNetworkRepository cfxNetworkRepository;
    private final FetchWalletInteract findDefaultWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;

    private Disposable transactionDisposable;

    private String tokenAddr;

    TransactionsViewModel(
            CfxNetworkRepository cfxNetworkRepository,
            FetchWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract) {
        this.cfxNetworkRepository = cfxNetworkRepository;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        transactionDisposable.dispose();
//        balanceDisposable.dispose();
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<CfxWallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<List<Transaction>> transactions() {
        return transactions;
    }

    public LiveData<Map<String, String>> defaultWalletBalance() {
        return defaultWalletBalance;
    }

    public void prepare(String token) {
        this.tokenAddr = token;
        progress.postValue(true);
        disposable = cfxNetworkRepository
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void fetchTransactions() {
        progress.postValue(true);
        transactionDisposable = Observable.interval(0, FETCH_TRANSACTIONS_INTERVAL, TimeUnit.MINUTES)
            .doOnNext(l ->
                disposable = fetchTransactionsInteract
                        .fetch(defaultWallet.getValue().address,  this.tokenAddr )
                        .subscribe(this::onTransactions, this::onError))
            .subscribe();
    }

    public void getBalance() {
//        balanceDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
//                .doOnNext(l -> getDefaultWalletBalance
//                        .get(defaultWallet.getValue())
//                        .subscribe(defaultWalletBalance::postValue, t -> {}))
//                .subscribe();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .findDefault()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(CfxWallet wallet) {
//        LogUtils.d("onDefaultWallet");
        defaultWallet.setValue(wallet);
//        getBalance();
        fetchTransactions();
    }

    private void onTransactions(Transaction[] transactions) {
        progress.postValue(false);

        // ETH transfer ingore the contract call
        if (TextUtils.isEmpty(tokenAddr)) {
            ArrayList<Transaction> transactionList = new ArrayList<>();
//            LogUtils.d("size:" + transactionList.size());
            for (Transaction t: transactions) {
                if (t.contractCreated == null || t.contractCreated == "null") {
                    transactionList.add(t);
                }
            }
            this.transactions.postValue(transactionList);
        } else {
            this.transactions.postValue(Arrays.asList(transactions));
        }


    }

}
