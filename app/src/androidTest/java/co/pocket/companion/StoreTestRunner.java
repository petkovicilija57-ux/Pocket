package co.pocket.companion;

import android.app.Instrumentation;
import android.os.Bundle;

public final class StoreTestRunner extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private void check(boolean pass,String label){if(!pass)throw new AssertionError(label);}
    @Override public void onStart() {
        Bundle result=new Bundle();
        String name="pocket-verification.db";
        getTargetContext().deleteDatabase(name);
        try {
            check(Money.parse("12,50")==1250,"decimal comma");
            boolean invalid=false;try{Money.parse("1.2.3");}catch(IllegalArgumentException e){invalid=true;}check(invalid,"malformed money rejected");
            String hash=PasswordKdf.hash("Pocket-test-password".toCharArray());
            check(PasswordKdf.verify("Pocket-test-password".toCharArray(),hash),"correct password");
            check(!PasswordKdf.verify("wrong password".toCharArray(),hash),"wrong password");
            try(PocketDb db=new PocketDb(getTargetContext(),name)) {
                db.savePayment(null,1250,"Shop","Hrana",1000);
                PocketDb.Payment p=db.payments().get(0);
                db.savePayment(p.id(),2000,"Store","Racuni",2000);
                check(db.payments().size()==1&&db.payments().get(0).cents()==2000,"edit without duplicate");
                db.setBudget("2026-09",10000);db.setBudget("2026-09",15000);
                check(db.budget("2026-09")==15000&&db.budget("2026-08")==0,"budget per month");
                db.saveGoal(null,"Monitor",22500,5000);
                PocketDb.Goal goal=db.goals().get(0);db.saveGoal(goal.id(),goal.name(),22500,7500);
                check(db.goals().size()==1&&db.goals().get(0).saved()==7500,"goal edit");
                db.saveSubscription(null,"Internet",1500,5000);
                PocketDb.Subscription sub=db.subscriptions().get(0);db.saveSubscription(sub.id(),sub.name(),1600,6000);
                check(db.subscriptions().size()==1&&db.subscriptions().get(0).due()==6000,"subscription edit");
                db.noteSource("bank.test","Bank",1000);db.noteSource("bank.test","Bank",2000);
                check(db.lastSeen("bank.test")==2000,"source updated");
            }
            try(PocketDb db=new PocketDb(getTargetContext(),name)) {
                check(db.payments().get(0).merchant().equals("Store"),"persistent payment");
                db.delete("payments",db.payments().get(0).id());check(db.payments().isEmpty(),"payment delete");
                db.delete("goals",db.goals().get(0).id());check(db.goals().isEmpty(),"goal delete");
                db.delete("subscriptions",db.subscriptions().get(0).id());check(db.subscriptions().isEmpty(),"subscription delete");
            }
            result.putString("stream","Pocket data tests passed\n");finish(-1,result);
        } catch(Throwable failure){result.putString("stream","FAILED: "+failure);finish(0,result);}
        finally {getTargetContext().deleteDatabase(name);}
    }
}
