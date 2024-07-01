import net.sf.jasperreports.engine.JRDefaultScriptlet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;

public class UtilScriptlet extends JRDefaultScriptlet {
	private boolean checkBeforeHour(Calendar start, int hour){
        Calendar startHour = (Calendar) start.clone();
        startHour.set(Calendar.MINUTE, 0);
        startHour.set(Calendar.SECOND, 0);
        startHour.set(Calendar.MILLISECOND, 0);
        startHour.set(Calendar.HOUR_OF_DAY, hour % 24);
        return startHour.compareTo(start) >= 0;
    }
	
	public int getNumDays(Date start, Date end, String hourStr){
		int hour = getHour(hourStr);
		Calendar c1 = GregorianCalendar.getInstance();
        c1.setTime(start);

        boolean beforeHour = checkBeforeHour(c1, hour);

        Calendar c2 = GregorianCalendar.getInstance();
        c2.setTime(end);

        c1.set(Calendar.MINUTE, 0);
        c1.set(Calendar.SECOND, 0);
        c1.set(Calendar.MILLISECOND, 0);
        c1.set(Calendar.HOUR_OF_DAY, 0);

        c2.set(Calendar.MINUTE, 0);
        c2.set(Calendar.SECOND, 0);
        c2.set(Calendar.MILLISECOND, 0);
        c2.set(Calendar.HOUR_OF_DAY, 0);

        if(c1.compareTo(c2) == 0){
            return 1;
        }

        c1.add(Calendar.DAY_OF_WEEK, (beforeHour ? 0 : 1));
        int w1 = c1.get(Calendar.DAY_OF_WEEK);
        w1 = (w1 - 2 + 7) % 7;
        c1.add(Calendar.DAY_OF_WEEK, -w1);
        int w2 = c2.get(Calendar.DAY_OF_WEEK);
        w2 = (w2 - 2 + 7) % 7;
        c2.add(Calendar.DAY_OF_WEEK, -w2);

        int days = (int) ((c2.getTimeInMillis() - c1.getTimeInMillis()) / (1000 * 60 * 60 * 24));
        int daysWithoutSunday = days - (days * 2 / 7);

        int result = daysWithoutSunday - ((w1 > 4) ? 4 : w1) + (w2 > 4 ? 4 : w2) + ((w1 <= 4) ? 1 : 0);
        if(result == 0){
            return 1;
        }
        return result;
    }
	
	public String getModules(List<String> strs){
		List<String> mods = new ArrayList<>();
		for(int i = 0; i < strs.size(); i++){
			if(strs.get(i).equals("Договоры")){
				mods.add("ContractsLite");
			} else if(strs.get(i).equals("ОРД")){
				mods.add("Missions");
			} else if(strs.get(i).equals("Внутренние")){
				mods.add("InternalDocs");
			} else {
				mods.add("OutputDocs");
			}
		}
		return String.join(",", mods);
	}
	
	public int getHour(String str){
		int res;
		try {
		     res = Integer.parseInt(str);
		     if(res > 24 || res < 0){
		    	 res = 15;
		     }
		} catch (NumberFormatException e) {
			res = 15;
		}
		return res;
	}
}