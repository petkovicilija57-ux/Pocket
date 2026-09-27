package co.pocket.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.*;
import java.time.*;
import java.util.*;

public final class MainActivity extends Activity {
    private boolean unlocked;
    private String page = "Pregled", search = "", category = "Sve";
    private YearMonth month = YearMonth.now();
    private AlertDialog dialog;
    private String exportText;
    private final String[] categories = {"Hrana", "Prevoz", "Skola", "Kupovina", "Zabava", "Racuni", "Other"};
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (!getSharedPreferences("pocket_settings",MODE_PRIVATE).getBoolean("registered",false)) {
            startActivity(new Intent(this,RegisterActivity.class)); finish();
        }
    }
    @Override protected void onResume() { super.onResume(); if (!isFinishing()) { if (unlocked) render(); else lock(); } }
    @Override protected void onPause() { unlocked=false; super.onPause(); }
    @Override protected void onStop() {
        unlocked = false;
        if (dialog != null) dialog.dismiss();
        setContentView(Ui.column(this));
        super.onStop();
    }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }

    private void lock() {
        unlocked = false;
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this,"Pocket"));
        l.addView(Ui.h2(this,"Otkljucaj"));
        EditText password = input(l,"Lozinka", "", false);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button button = Ui.button(this,"Otkljucaj",v -> {}); l.addView(button);
        button.setOnClickListener(v -> {
            long until = getSharedPreferences("pocket_settings",MODE_PRIVATE).getLong("locked_until",0);
            if (System.currentTimeMillis() < until) { message("Sacekaj jednu minutu pa pokusaj ponovo."); return; }
            button.setEnabled(false);
            char[] chars = password.getText().toString().toCharArray(); password.setText("");
            worker.execute(() -> {
                boolean accepted = false;
                try { String hash = new SecureStore(this).get("password_hash"); accepted = hash != null && PasswordKdf.verify(chars,hash); }
                catch (Exception ignored) {} finally { Arrays.fill(chars,'\0'); }
                final boolean ok = accepted;
                runOnUiThread(() -> {
                    if (isDestroyed() || !hasWindowFocus()) return;
                    button.setEnabled(true);
                    android.content.SharedPreferences prefs = getSharedPreferences("pocket_settings",MODE_PRIVATE);
                    if (ok) { prefs.edit().putInt("failed_login",0).apply(); unlocked=true; render(); }
                    else {
                        int failures=prefs.getInt("failed_login",0)+1;
                        prefs.edit().putInt("failed_login",failures).putLong("locked_until",failures%5==0 ? System.currentTimeMillis()+60000 : 0).apply();
                        message("Pogresna lozinka ili nedostupan sigurni zapis.");
                    }
                });
            });
        });
        l.addView(Ui.body(this,"Podaci su na ovom telefonu. Lozinka se ne moze obnoviti putem e-poste."));
        show(l);
    }

    private void show(LinearLayout l) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(l);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{ v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        setContentView(scroll);
    }
    private void render() {
        if (!unlocked) return;
        LinearLayout l=Ui.column(this); l.addView(Ui.title(this,"Pocket"));
        l.addView(Ui.button(this,page+"  /  Meni",v -> {
            String[] pages={"Pregled","Troskovi","Stednja","Pretplate","Izvjestaji","Postavke"};
            dialog=new AlertDialog.Builder(this).setTitle("Pocket").setItems(pages,(d,i)->{page=pages[i];render();}).show();
        }));
        try(PocketDb db=new PocketDb(this)) {
            switch(page) {
                case "Troskovi": expenses(l,db); break;
                case "Stednja": goals(l,db); break;
                case "Pretplate": subscriptions(l,db); break;
                case "Izvjestaji": reports(l,db); break;
                case "Postavke": settings(l); break;
                default: overview(l,db);
            }
        }
        show(l);
    }
    private List<PocketDb.Payment> monthly(PocketDb db) {
        List<PocketDb.Payment> rows=new ArrayList<>();
        for(PocketDb.Payment p:db.payments()) if(YearMonth.from(Instant.ofEpochMilli(p.time()).atZone(ZoneId.systemDefault())).equals(month)) rows.add(p);
        return rows;
    }
    private long spent(PocketDb db) { long total=0; for(PocketDb.Payment p:monthly(db)) total=Math.addExact(total,p.cents()); return total; }
    private void monthControl(LinearLayout l) {
        LinearLayout row=new LinearLayout(this);
        Button prev=Ui.button(this,"<",v->{month=month.minusMonths(1);render();});
        Button next=Ui.button(this,">",v->{month=month.plusMonths(1);render();});
        row.addView(prev,new LinearLayout.LayoutParams(0,-2,1)); row.addView(Ui.body(this,month.toString()),new LinearLayout.LayoutParams(0,-2,2)); row.addView(next,new LinearLayout.LayoutParams(0,-2,1)); l.addView(row);
    }
    private void overview(LinearLayout l,PocketDb db) {
        monthControl(l); long total=spent(db),limit=db.budget(month.toString());
        l.addView(Ui.h2(this,"Potroseno: "+Money.text(total)));
        l.addView(Ui.body(this,limit==0 ? "Budzet nije postavljen." : "Budzet: "+Money.text(limit)+"\nPreostalo: "+Money.text(limit-total)));
        if(limit>0 && month.equals(YearMonth.now())) l.addView(Ui.body(this,"Dnevno do kraja mjeseca: "+Money.text(Math.max(0,limit-total)/(month.lengthOfMonth()-LocalDate.now().getDayOfMonth()+1))));
        l.addView(Ui.button(this,"Postavi budzet",v->{
            LinearLayout form=Ui.column(this); EditText amount=input(form,"Budzet u KM",Money.input(limit),true);
            form("Mjesecni budzet",form,()->{try(PocketDb data=new PocketDb(this)){data.setBudget(month.toString(),Money.parse(amount.getText().toString()));}});
        }));
        l.addView(Ui.button(this,"Dodaj trosak",v->editPayment(null)));
        l.addView(Ui.h2(this,"Pretplate u narednih 7 dana")); boolean any=false;
        for(PocketDb.Subscription sub:db.subscriptions()) if(sub.due()<=System.currentTimeMillis()+7L*86400000){any=true;l.addView(Ui.body(this,sub.name()+" - "+Money.text(sub.cents())+" - "+date(sub.due())));}
        if(!any) l.addView(Ui.body(this,"Nema dospjelih pretplata."));
    }
    private void expenses(LinearLayout l,PocketDb db) {
        monthControl(l);
        EditText query=input(l,"Pretrazi naziv",search,false);
        l.addView(Ui.button(this,"Pretrazi",v->{search=query.getText().toString().trim();render();}));
        l.addView(Ui.button(this,"Kategorija: "+category,v->{
            String[] all=new String[categories.length+1];all[0]="Sve";System.arraycopy(categories,0,all,1,categories.length);
            dialog=new AlertDialog.Builder(this).setItems(all,(d,i)->{category=all[i];render();}).show();
        }));
        l.addView(Ui.button(this,"Dodaj trosak",v->editPayment(null)));
        boolean any=false;
        for(PocketDb.Payment p:monthly(db)) {
            if(!p.merchant().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)) || (!category.equals("Sve")&&!category.equals(p.category()))) continue;
            any=true;
            l.addView(Ui.button(this,p.merchant()+"  "+Money.text(p.cents())+"\n"+p.category()+" - "+date(p.time()),v->{
                dialog=new AlertDialog.Builder(this).setTitle(p.merchant()).setItems(new String[]{"Uredi","Obrisi"},(d,i)->{if(i==0)editPayment(p);else remove("payments",p.id());}).show();
            }));
        }
        if(!any)l.addView(Ui.body(this,"Nema troskova za ovaj izbor."));
    }
    private void editPayment(PocketDb.Payment p) {
        LinearLayout f=Ui.column(this);
        EditText name=input(f,"Naziv troska",p==null?"":p.merchant(),false);
        EditText amount=input(f,"Iznos KM",p==null?"":Money.input(p.cents()),true);
        Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories));f.addView(cat);
        if(p!=null)cat.setSelection(Math.max(0,Arrays.asList(categories).indexOf(p.category())));
        long[] time={p==null?System.currentTimeMillis():p.time()}; dateButton(f,time);
        form("Trosak",f,()->{try(PocketDb db=new PocketDb(this)){db.savePayment(p==null?null:p.id(),Money.parse(amount.getText().toString()),name.getText().toString().trim(),cat.getSelectedItem().toString(),time[0]);}});
    }
    private void goals(LinearLayout l,PocketDb db) {
        l.addView(Ui.h2(this,"Ciljevi stednje"));l.addView(Ui.button(this,"Dodaj cilj",v->editGoal(null)));
        if(db.goals().isEmpty())l.addView(Ui.body(this,"Jos nema ciljeva stednje."));
        for(PocketDb.Goal g:db.goals()) {
            l.addView(Ui.button(this,g.name()+"\n"+Money.text(g.saved())+" / "+Money.text(g.target()),v->{
                dialog=new AlertDialog.Builder(this).setTitle(g.name()).setItems(new String[]{"Uredi iznose","Obrisi"},(d,i)->{if(i==0)editGoal(g);else remove("goals",g.id());}).show();
            }));
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgress((int)Math.min(100,g.saved()*100.0/g.target()));l.addView(progress);
        }
    }
    private void editGoal(PocketDb.Goal g) {
        LinearLayout f=Ui.column(this);EditText name=input(f,"Naziv cilja",g==null?"":g.name(),false);
        EditText target=input(f,"Cilj KM",g==null?"":Money.input(g.target()),true),saved=input(f,"Vec ustedjeno KM",g==null?"0":Money.input(g.saved()),true);
        form("Stednja",f,()->{try(PocketDb db=new PocketDb(this)){db.saveGoal(g==null?null:g.id(),name.getText().toString().trim(),Money.parse(target.getText().toString()),Money.parse(saved.getText().toString()));}});
    }
    private void subscriptions(LinearLayout l,PocketDb db) {
        l.addView(Ui.h2(this,"Mjesecne pretplate"));l.addView(Ui.button(this,"Dodaj pretplatu",v->editSubscription(null)));
        if(db.subscriptions().isEmpty())l.addView(Ui.body(this,"Nema pretplata."));
        for(PocketDb.Subscription sub:db.subscriptions())l.addView(Ui.button(this,sub.name()+"  "+Money.text(sub.cents())+"\nDospijeva: "+date(sub.due()),v->{
            dialog=new AlertDialog.Builder(this).setTitle(sub.name()).setItems(new String[]{"Uredi","Evidentiraj placanje","Obrisi"},(d,i)->{
                if(i==0)editSubscription(sub);else if(i==2)remove("subscriptions",sub.id());else {
                    dialog=new AlertDialog.Builder(this).setMessage("Dodati trosak i pomjeriti rok za jedan mjesec? Ovo ne izvrsava placanje.").setPositiveButton("Evidentiraj",(a,b)->{
                        try(PocketDb data=new PocketDb(this)){
                            android.database.sqlite.SQLiteDatabase sql=data.getWritableDatabase();sql.beginTransaction();
                            try{data.savePayment(null,sub.cents(),sub.name(),"Racuni",System.currentTimeMillis());data.saveSubscription(sub.id(),sub.name(),sub.cents(),Instant.ofEpochMilli(sub.due()).atZone(ZoneId.systemDefault()).plusMonths(1).toInstant().toEpochMilli());sql.setTransactionSuccessful();}finally{sql.endTransaction();}
                        }render();
                    }).setNegativeButton("Otkazi",null).show();
                }
            }).show();
        }));
    }
    private void editSubscription(PocketDb.Subscription sub) {
        LinearLayout f=Ui.column(this);EditText name=input(f,"Naziv pretplate",sub==null?"":sub.name(),false),amount=input(f,"Mjesecni iznos KM",sub==null?"":Money.input(sub.cents()),true);
        long[] due={sub==null?System.currentTimeMillis():sub.due()};dateButton(f,due);
        form("Pretplata",f,()->{try(PocketDb db=new PocketDb(this)){db.saveSubscription(sub==null?null:sub.id(),name.getText().toString().trim(),Money.parse(amount.getText().toString()),due[0]);}});
    }
    private void reports(LinearLayout l,PocketDb db) {
        monthControl(l);l.addView(Ui.h2(this,"Ukupno: "+Money.text(spent(db))));
        Map<String,Long> totals=new TreeMap<>();for(PocketDb.Payment p:monthly(db))totals.merge(p.category(),p.cents(),Long::sum);
        for(Map.Entry<String,Long> e:totals.entrySet())l.addView(Ui.body(this,e.getKey()+": "+Money.text(e.getValue())));
        if(totals.isEmpty())l.addView(Ui.body(this,"Nema podataka za ovaj mjesec."));
        l.addView(Ui.button(this,"Izvezi mjesec u CSV",v->{
            StringBuilder csv=new StringBuilder("Datum,Naziv,Kategorija,Iznos,Valuta\r\n");
            try(PocketDb data=new PocketDb(this)){for(PocketDb.Payment p:monthly(data))csv.append(date(p.time())).append(',').append(csv(p.merchant())).append(',').append(csv(p.category())).append(',').append(Money.input(p.cents())).append(",BAM\r\n");}
            exportText=csv.toString();
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv").putExtra(Intent.EXTRA_TITLE,"Pocket-"+month+".csv"),7);
        }));
    }
    private static String csv(String value){return "\"'"+value.replace("\"","\"\"")+"\"";}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==7&&result==RESULT_OK&&data!=null&&data.getData()!=null&&exportText!=null){try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException();out.write(exportText.getBytes(java.nio.charset.StandardCharsets.UTF_8));message("CSV sacuvan");}catch(Exception e){message("Izvoz nije uspio");}}exportText=null;}

    private void settings(LinearLayout l) {
        l.addView(Ui.h2(this,"Obavijesti i privatnost"));
        l.addView(Ui.body(this,"Pristup obavijestima: "+(NotificationHealth.listenerEnabled(this)?"dozvoljen":"nije dozvoljen")));
        l.addView(Ui.body(this,"Google Wallet: "+NotificationHealth.status(this,SourceRules.GOOGLE_WALLET,"Google Wallet").summary()));
        l.addView(Ui.body(this,"PayPal obavijesti: "+NotificationHealth.status(this,SourceRules.PAYPAL,"PayPal").summary()));
        String bank=getSharedPreferences("pocket_settings",MODE_PRIVATE).getString("bank_package","");
        l.addView(Ui.body(this,bank.isEmpty()?"Banka nije odabrana":"Banka: "+NotificationHealth.status(this,bank,"Bank").summary()));
        l.addView(Ui.button(this,"Odaberi bankovnu aplikaciju",v->selectBank()));
        l.addView(Ui.button(this,"Pristup obavijestima",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        l.addView(Ui.body(this,"Uvoz prepoznaje neke obavijesti o kupovini u KM/BAM. Provjeri iznose i ispravi pogresne zapise. Dozvola nije dokaz da banka salje obavijesti."));
        l.addView(Ui.body(this,"Placanje karticom, PayPal prijava i NFC placanje nisu dostupni. Pocket vodi lokalnu evidenciju i ne prenosi novac."));
        l.addView(Ui.button(this,"Zakljucaj",v->lock()));
    }
    private void selectBank(){
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        TreeMap<String,String> apps=new TreeMap<>();
        for(android.content.pm.ResolveInfo app:getPackageManager().queryIntentActivities(intent,0))if(!app.activityInfo.packageName.equals(getPackageName()))apps.put(app.loadLabel(getPackageManager())+" ("+app.activityInfo.packageName+")",app.activityInfo.packageName);
        String[] labels=apps.keySet().toArray(new String[0]);
        dialog=new AlertDialog.Builder(this).setTitle("Bankovna aplikacija").setItems(labels,(d,i)->{getSharedPreferences("pocket_settings",MODE_PRIVATE).edit().putString("bank_package",apps.get(labels[i])).apply();render();}).setNeutralButton("Ukloni izbor",(d,i)->{getSharedPreferences("pocket_settings",MODE_PRIVATE).edit().remove("bank_package").apply();render();}).setNegativeButton("Otkazi",null).show();
    }
    private EditText input(LinearLayout l,String hint,String value,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setContentDescription(hint);e.setText(value);e.setSingleLine(true);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);l.addView(e);return e;}
    private void form(String title,LinearLayout body,Runnable save){ScrollView s=new ScrollView(this);s.addView(body);dialog=new AlertDialog.Builder(this).setTitle(title).setView(s).setPositiveButton("Sacuvaj",null).setNegativeButton("Otkazi",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{save.run();dialog.dismiss();render();}catch(IllegalArgumentException e){message(e.getMessage());}catch(Exception e){message("Cuvanje nije uspjelo. Pokusaj ponovo.");}}));dialog.show();}
    private void remove(String table,String id){dialog=new AlertDialog.Builder(this).setMessage("Obrisati ovaj zapis?").setPositiveButton("Obrisi",(d,i)->{try(PocketDb db=new PocketDb(this)){db.delete(table,id);}render();}).setNegativeButton("Otkazi",null).show();}
    private void dateButton(LinearLayout l,long[] value){Button b=Ui.button(this,date(value[0]),v->{});b.setOnClickListener(v->{LocalDate now=Instant.ofEpochMilli(value[0]).atZone(ZoneId.systemDefault()).toLocalDate();DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)->{value[0]=LocalDate.of(y,m+1,d).atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();b.setText(date(value[0]));},now.getYear(),now.getMonthValue()-1,now.getDayOfMonth());picker.show();});l.addView(b);}
    private String date(long time){return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().toString();}
    private void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
