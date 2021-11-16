package smu.smc.jiaming.scheme1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinerManageThread extends Thread{
    private List<MinerThread> miners;
    private Transactions transactions;

    public MinerManageThread(Transactions transactions, List<MinerThread> miners){
        this.transactions = transactions;
        this.miners = miners;
    }

    public MinerManageThread(Transactions transactions){
        this.transactions = transactions;
        this.miners = new ArrayList<>();
    }

    public void addMiner(MinerThread miner, boolean flag){
        this.miners.add(miner);
        if (flag) miner.start();
    }

    public void removeMiner(MinerThread miner){
        this.miners.remove(miner);
        miner.interrupt();
    }

    public void starts(){
        for (MinerThread miner: miners) {
            miner.start();
        }
        this.start();
    }

    public void stops(){
        for (MinerThread miner: miners) {
            miner.interrupt();
        }
    }

    public boolean broadcast(Block block) {
        boolean flag = true;
        for (MinerThread miner: miners) {
            try {
                flag = flag && miner.acceptBlock(block.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public void broadcast(TX tx){
        for (MinerThread miner: miners) {
            miner.recieveTX(tx);
        }
    }

    public void broadcast(TX[] tx){
        for (MinerThread miner: miners) {
            miner.recieveTXes(tx);
        }
    }

    @Override
    public void run(){
        while(true && !isInterrupted()){
            try {
                synchronized (this.transactions) {
                    this.transactions.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
            if(transactions.flag) {
                stops();
                return;
            }
            if(transactions.txes.size() == 1) {
                this.broadcast(transactions.txes.get(0));
            }
            else {
                TX[] tx = new TX[this.transactions.txes.size()];
                this.broadcast((TX[]) this.transactions.txes.toArray(tx));
                this.transactions.txes.clear();
            }
        }
    }

    public static class Transactions {
        private List<TX> txes = new ArrayList<>();
        private boolean flag = false;

        public synchronized void add(TX txes){
            this.txes.add(txes);
            this.notifyAll();
        }
        public synchronized void add(TX[] txes){
            this.txes.addAll(Arrays.asList(txes));
            this.notifyAll();
        }

        public synchronized void stop(){
            this.flag = true;
            this.notifyAll();
        }
    }
}
