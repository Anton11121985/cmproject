import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ru.intertrust.cm.core.business.api.CollectionsService;
import ru.intertrust.cm.core.business.api.dto.DateTimeValue;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObject;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObjectCollection;
import ru.intertrust.cm.core.business.api.dto.ReferenceValue;
import ru.intertrust.cm.core.business.api.dto.Value;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;
import ru.intertrust.cm.core.service.api.ReportDS;


public class DataSet implements ReportDS {
       
    List<ReportRow> result = new ArrayList<ReportRow>();
    
    public JRDataSource getJRDataSource(Connection connection, Map params) throws SQLException  {               

    	ReportRow row = null;
    	if (params.containsKey("Path")) {
    		String path = (String) params.get("Path");
    		try {
                File file = new File(path);
                FileReader fr = new FileReader(file);
                BufferedReader reader = new BufferedReader(fr);
                String line = reader.readLine();
                while (line != null) {
                	row = new ReportRow();
                	String aRow[] = line.split("~", -2);
                	try {
                		row.setcolumn1(aRow[0]);
                		row.setcolumn2(aRow[1]);
                		row.setcolumn3(aRow[2]);
                		row.setcolumn4(aRow[3]);
                		row.setcolumn4hid(aRow[3]);
                		row.setcolumn5(aRow[5]);
                    	row.setcolumn6(aRow[6]);
                    	row.setcolumn7("");
                    	row.setcolumn8(aRow[7]);
                    	row.setcolumn9(aRow[8]);
                    	row.setcolumn10(aRow[9]);
                    	row.setcolumn11(aRow[10]);
                    	row.setcolumn12(aRow[11]);
                    	row.setcolumn13(aRow[12]);
                    	row.setcolumn14(aRow[13]);
                    	row.setcolumn14hid(aRow[13]);
                    	row.setcolumn15("");
                    	row.setcolumn16(aRow[16]);
                    	row.setcolumn17(aRow[17]);
                    	
                    	result.add(row);
                	} catch (ArrayIndexOutOfBoundsException e) {
                		// e.printStackTrace();		
                	}
                    line = reader.readLine();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    	}
    	
        return new JRBeanCollectionDataSource(result);
    }
    
    public class ReportRow {
		private String column1; // Регистрационный номер
		private String column2; // Дата  документа по обращению
		private String column3; // Дата поступления в орган
		private String column4; // Дата регистрации в органе
		private String column4hid; // Заявитель; Адрес;  e-mail:
		private String column5; // Код вопроса
		private String column6; // «Направлен по компетенции» Название организации, в которую направлен вопрос.
		private String column7; // «Направлен по компетенции» Не заполнять пока
		private String column8; // «Направлен по компетенции»
		private String column9; // «Направлен по компетенции» рег. номер сопроводительного документа
		private String column10;// «Направлен по компетенции», рег. дата сопроводительного документа.
		private String column11;// «Рассмотрено. Поддержано», то выводится «+», иначе пусто.
		private String column12;// «Меры приняты» = «ДА», то выводится «+», иначе пусто.
		private String column13;// «Рассмотрено. Разъяснено», то выводится «+», иначе пусто.
		private String column14;// «Дан ответ автору», то выводится «+», иначе пусто.
		private String column14hid;// Выводятся имена файлов, выгруженных по данному вопросу на ССТУ
		private String column15;// Содержание поля «Дата ответа».
		private String column16;// «Оставлено без ответа автору», то выводится «+», иначе пусто.
		private String column17;// Заполняется знаком «+», если заполнен столбец 4
		
		public String getcolumn1() {
			return column1;
		}
		public void setcolumn1(String s) {
			this.column1 = s;
		}
		public String getcolumn2() {
			return column2;
		}
		public void setcolumn2(String s) {
			this.column2 = s;
		}
		public String getcolumn3() {
			return column3;
		}
		public void setcolumn3(String s) {
			this.column3 = s;
		}
		public String getcolumn4() {
			return column4;
		}
		public void setcolumn4(String s) {
			this.column4 = s;
		}
		public String getcolumn4hid() {
			return column4hid;
		}
		public void setcolumn4hid(String s) {
			this.column4hid = s;
		}
		public String getcolumn5() {
			return column5;
		}
		public void setcolumn5(String s) {
			this.column5 = s;
		}
		public String getcolumn6() {
			return column6;
		}
		public void setcolumn6(String s) {
			this.column6 = s;
		}
		public String getcolumn7() {
			return column7;
		}
		public void setcolumn7(String s) {
			this.column7 = s;
		}
		public String getcolumn8() {
			return column8;
		}
		public void setcolumn8(String s) {
			this.column8 = s;
		}
		public String getcolumn9() {
			return column9;
		}
		public void setcolumn9(String s) {
			this.column9 = s;
		}
		public String getcolumn10() {
			return column10;
		}
		public void setcolumn10(String s) {
			this.column10 = s;
		}
		public String getcolumn11() {
			return column11;
		}
		public void setcolumn11(String s) {
			this.column11 = s;
		}
		public String getcolumn12() {
			return column12;
		}
		public void setcolumn12(String s) {
			this.column12 = s;
		}
		public String getcolumn13() {
			return column13;
		}
		public void setcolumn13(String s) {
			this.column13 = s;
		}
		public String getcolumn14() {
			return column14;
		}
		public void setcolumn14(String s) {
			this.column14 = s;
		}
		public String getcolumn14hid() {
			return column14hid;
		}
		public void setcolumn14hid(String s) {
			this.column14hid = s;
		}
		public String getcolumn15() {
			return column15;
		}
		public void setcolumn15(String s) {
			this.column15 = s;
		}
		public String getcolumn16() {
			return column16;
		}
		public void setcolumn16(String s) {
			this.column16 = s;
		}
		public String getcolumn17() {
			return column17;
		}
		public void setcolumn17(String s) {
			this.column17 = s;
		}
		
		ReportRow() {
		}
	
	}	
}