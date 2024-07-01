import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import ru.intertrust.cm.core.business.api.CollectionsService;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObject;
import ru.intertrust.cm.core.business.api.dto.IdentifiableObjectCollection;
import ru.intertrust.cm.core.business.api.ReportMergeService;
import ru.intertrust.cm.core.business.impl.report.ReportBuilderFormats;
import ru.intertrust.cm.core.config.model.ReportMetadataConfig;
import ru.intertrust.cm.core.model.ReportServiceException;
import ru.intertrust.cm.core.service.api.ReportGenerator;
import ru.intertrust.cmj.af.utils.BeansUtils;
import ru.intertrust.cmj.af.core.AFEntityStorage;
import ru.intertrust.cmj.af.core.AFCMDomino;
import ru.intertrust.cmj.dp.DPM.RKK;
import ru.intertrust.cmj.dp.DPM;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import lotus.domino.Document;
import lotus.domino.NotesException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.nio.charset.StandardCharsets;

import static ru.intertrust.cmj.af.core.AFSession.Manual.close;
import static ru.intertrust.cmj.af.core.AFSession.Manual.defineLocalUser;
import static ru.intertrust.cmj.af.core.AFSession.isDefinedOrOpened;

public class InputDocumentRkk implements ReportGenerator {
    private final Logger logger = LoggerFactory.getLogger("input-document-rkk.InputDocumentRkk");
    String mainfileName = "input-document-rkk.docx";
    String sql1FileName = "sql1.sql";
    String resSqlFileName = "resolution.sql";
    String reportsSqlFileName = "reports.sql";
    String reasonTransferSqlFileName = "reportsReasonTransfer.sql";
    @Autowired
    private CollectionsService collectionsService;
    @Autowired
    private ReportMergeService reportMergeService;

    private XWPFDocument document;

    public void HTMLtoDOCX(XWPFDocument document, XWPFParagraph paragraph, String html, String docxPath) throws Exception {

        this.document = document;
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        ParagraphNodeVisitor prn = new ParagraphNodeVisitor(paragraph);
        String[] strings = html.split("split");

        for (String string : strings) {

            if (string.startsWith("/")) {
                string = string.replace("/", "");
                prn.tail(string);
            } else {
                prn.head(string);
            }
        }
        FileOutputStream out = new FileOutputStream(docxPath);
        document.write(out);
        out.close();
        document.close();

    }

    private class ParagraphNodeVisitor {

        String nodeName;
        boolean needNewRun;
        boolean isItalic;
        boolean isBold;
        boolean isUnderlined;
        boolean isBreak;
        int fontSize;
        String fontColor;
        XWPFParagraph paragraph;
        XWPFRun run;

        ParagraphNodeVisitor(XWPFParagraph paragraph) {
            this.paragraph = paragraph;
            this.run = paragraph.createRun();
            this.nodeName = "";
            this.needNewRun = false;
            this.isItalic = false;
            this.isBold = false;
            this.isUnderlined = false;
            this.isBreak = false;
            this.fontSize = 12;
            this.fontColor = "000000";
        }

        public void head(String node) {
            nodeName = node;

            needNewRun = false;
            if ("i".equals(nodeName)) {
                isItalic = true;
            } else if ("b".equals(nodeName)) {
                isBold = true;
            } else if ("u".equals(nodeName)) {
                isUnderlined = true;
            } else if ("p".equals(nodeName)) {
                run.addBreak();
            } else {
                run.setText(node);
                run.setFontSize(fontSize);
                run.setFontFamily("Times New Roman");
                needNewRun = true; //after setting the text in the run a new run is needed
            }
            if (needNewRun)
                run = paragraph.createRun();
            needNewRun = false;
            run.setItalic(isItalic);
            run.setBold(isBold);
            if (isUnderlined)
                run.setUnderline(UnderlinePatterns.SINGLE);
            else
                run.setUnderline(UnderlinePatterns.NONE);
            run.setColor(fontColor);
        }

        public void tail(String node) {
            nodeName = node;

            if ("i".equals(nodeName)) {
                isItalic = false;
            } else if ("b".equals(nodeName)) {
                isBold = false;
            } else if ("u".equals(nodeName)) {
                isUnderlined = false;
            } else if ("p".equals(nodeName)) {
                isBreak = false;
            }

            if (needNewRun)
                run = paragraph.createRun();
            needNewRun = false;
            run.setItalic(isItalic);
            run.setBold(isBold);

            if (isUnderlined)
                run.setUnderline(UnderlinePatterns.SINGLE);
            else
                run.setUnderline(UnderlinePatterns.NONE);
            run.setColor(fontColor);
        }
    }

    @Override
    public InputStream generate(ReportMetadataConfig reportMetadata, File templateFolder, Map<String, Object> parameters) {
        final List<File> files = new ArrayList<>();
		final boolean sessionAlreadyOpened = isDefinedOrOpened();
        if (!sessionAlreadyOpened) {
            defineLocalUser();
        }
        try {
            logger.debug("Start generate report input-document-rkk");
            File sqlFile1 = new File(templateFolder, sql1FileName);

            String request_id = getRequestId(parameters);
            String query1 = queryString1(sqlFile1, "$request_id", request_id);
            logger.debug("query1=" + query1);

            IdentifiableObjectCollection collection = getCollection(query1);

            for (IdentifiableObject io : collection) {
                File file = new File(templateFolder, mainfileName);

                Map<String, String> map = createMap(templateFolder, io);
				Map<String, List<String>> mapL = createMapL(templateFolder, io);
                String resolution = map.get("«Резолюция»");
                List<String> reasonTransfer = mapL.get("«Причина переноса»");
				List<String> oldDate = mapL.get("«Cтарый срок»");
                map.remove("«Резолюция»");
                mapL.remove("«Причина переноса»");
				mapL.remove("«Cтарый срок»");
                File filenew = File.createTempFile("new1", ".docx");
                DotxTemplateFiller(file, filenew.getPath(), map);
                logger.debug("generate resolution=" + resolution);
                getPositionParagrForDelete(filenew, resolution, filenew.getPath());
				getPositionParagrForDeleteInTable(filenew, oldDate, reasonTransfer, filenew.getPath());

                files.add(filenew);
            }

            return this.reportMergeService.mergeReports(
                    new ArrayList<String>(Arrays.asList("DOCX")),
                    files, File.createTempFile("SochiServer_", "_report"));
        } catch (Exception ex) {
            throw new ReportServiceException("Error generate report", ex);
        } finally {
            if (!sessionAlreadyOpened) {
                close();
            }
            files.forEach(File::delete);
        }
    }
	
	private Map<String, List<String>> createMapL(File templateFolder, IdentifiableObject identifiableObject) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            String docid = identifiableObject.getString("docid");

            List<String> reasonTransfer = addReasonTransfer(templateFolder, docid, "");
            List<String> oldDate = addOldDate(templateFolder, docid, "");
            map.put("«Причина переноса»", reasonTransfer);
            map.put("«Cтарый срок»", oldDate);

        } catch (Exception ex) {
            throw new ReportServiceException("Error generate report1", ex);
        }
        return map;
    }

    private Map<String, String> createMap(File templateFolder, IdentifiableObject identifiableObject) {
        Map<String, String> map = new HashMap<>();
        try {
            map = fill(identifiableObject);
            String docid = identifiableObject.getString("docid");
            String docidtype = identifiableObject.getString("docidtype");
            String resolution = addResolution(templateFolder, docid, docidtype, "", 0);
            logger.debug("resolution=" + resolution);
            map.put("«Резолюция»", resolution);

        } catch (Exception ex) {
            throw new ReportServiceException("Error generate report1", ex);
        }
        return map;
    }

    private String addReportIfNoResolution(IdentifiableObjectCollection collection2, File templateFolder, String docid, String docidtype) {
        logger.debug("Start addReportIfNoResolution");
        String resolution = "";
        String resAuthorid = "";
        for (IdentifiableObject identifiableObject : collection2) {
            if (resAuthorid.isEmpty()) {
                resAuthorid = identifiableObject.getString("authorid");
            } else {
                resAuthorid = resAuthorid + "," + identifiableObject.getString("authorid");
            }
        }
        logger.debug("resAuthorid=" + resAuthorid);
        if (!resAuthorid.isEmpty()) {
            logger.debug("resAuthorid not empty");
            resolution = resolution + addAddresseeReports(templateFolder, docid, docidtype, resAuthorid, "-2");
        } else {
            logger.debug("resAuthorid is empty");
            resolution = resolution + addAddresseeReports(templateFolder, docid, docidtype, "-1", "-3");
        }
        if (!resolution.isEmpty()) {
            resolution = resolution + "splitpsplit" + "split/psplit";
        }
        return resolution;
    }

    private String addResolution(File templateFolder, String docid, String docidtype, String margin, int step) {
        String resolution = "";
        if (step == 0) {
            margin = "";
        } else {
            margin = margin + "     ";
        }
        step++;
        File sqlFile2 = new File(templateFolder, resSqlFileName);
        String query2 = queryString4(sqlFile2, "$id", docid, "$type", docidtype);
        logger.debug("addResolution query2=" + query2);
        IdentifiableObjectCollection collection2 = getCollection(query2);
		String authorid = "";

        if (step == 1) {
            resolution = addReportIfNoResolution(collection2, templateFolder, docid, docidtype);
            logger.debug("step=1, resolution=" + resolution);
        }
        for (IdentifiableObject identifiableObject : collection2) {
            if (identifiableObject.getString("authorName") == null) {
                logger.debug("authorName is null");
                return resolution;
            }
            String resid = identifiableObject.getString("resid");
            String residtype = identifiableObject.getString("residtype");
            resolution = resolution 
				+ (margin.isEmpty() && !identifiableObject.getString("authorid").equals(authorid) ?
                    addAddresseeReports(templateFolder, docid, docidtype, identifiableObject.getString("authorid"), "-1") : "")
				+ margin + "Резолюция: " + identifiableObject.getString("resDate") + " " + identifiableObject
                    .getString("authorName") + " ->" + " "
                    + (!StringUtils.isBlank(identifiableObject.getString("ExecResp")) ?
                    "splitusplit" + identifiableObject.getString("ExecResp") + "split/usplit" :
                    "")
                    + (!StringUtils.isBlank(identifiableObject.getString("ExecCurr")) ? ", " : "") + identifiableObject
                    .getString("ExecCurr") + " "
                    + "splitpsplit" + "splitbsplit" + margin + identifiableObject.getString("resolution") + "split/bsplit" + "split/psplit"
                    + (identifiableObject.getString("ctrldeadline").isEmpty() ?
                                                    "" : "splitpsplit" + margin + "Срок " + identifiableObject.getString("ctrldeadline") + "split/psplit")
                    + addreports(templateFolder, resid, residtype, margin + "     ")

                    + "splitpsplit" + "" + "split/psplit"

                    + "splitpsplit" + addResolution(templateFolder, resid, residtype, margin, step) + "split/psplit";
            logger.debug("addResolution resid=" + resid + ",step=" + step);
            logger.debug("addResolution resolution=" + resolution);
            authorid = identifiableObject.getString("authorid");
        }
        logger.debug("addResolution end");
        return resolution;
    }
	
	private String addAddresseeReports(File templateFolder, String resid, String residtype, String authorid, String param) {
		String reports = "";
        File sqlFile = new File(templateFolder, reportsSqlFileName);
        String query = queryString4(sqlFile, "$id", resid, "$type", residtype);
		query = query.replace("$authorid", authorid);
        query = query.replace("$param", param);
        logger.debug("addAddresseeReports query=" + query);
        IdentifiableObjectCollection collection = getCollection(query);
		
		for (IdentifiableObject identifiableObject : collection) {
			reports = reports + "splitisplit" + "Исполнение: "
                + identifiableObject.getString("execdate") + " - "
                + identifiableObject.getString("author") + " "
                + identifiableObject.getString("text") + "split/isplit"
				+ "splitpsplit" + "split/psplit";
		}
		return reports;	
	}

    private String addreports(File templateFolder, String resid, String residtype, String margin) {
        String reports = "";
        File sqlFile = new File(templateFolder, reportsSqlFileName);
        String query = queryString4(sqlFile, "$id", resid, "$type", residtype);
		query = query.replace("$authorid", "-1");
        query = query.replace("$param", "-3");
        IdentifiableObjectCollection collection = getCollection(query);

        for (IdentifiableObject identifiableObject : collection) {
            reports = reports + (!StringUtils.isBlank(identifiableObject.getString("author")) ?
                    "splitpsplit" + "splitisplit" + margin + "Исполнение: "
                            + identifiableObject.getString("execdate") + " - "
                            + identifiableObject.getString("author") + " "
                            + identifiableObject.getString("text") + " " + "split/psplit" + "split/isplit": "");
        }
        return reports;
    }

    private List<String> addReasonTransfer (File templateFolder, String resid, String margin) {
        List<String> reasonTransfer = new ArrayList();
        File sqlFile3 = new File(templateFolder, reasonTransferSqlFileName);
        String query3 = queryString1(sqlFile3, "$id", resid);
        IdentifiableObjectCollection collection3 = getCollection(query3);

        for (IdentifiableObject identifiableObject : collection3) {
			String unid = identifiableObject.getString("unid");
			String res = "";
			if (unid == null || unid.isEmpty()) {
				res = identifiableObject.getString("transfertable");
			}
			else {
				DPM.RKK doc = AFEntityStorage.getEntityByUNID(unid);
				if (doc == null) {
					res = "";
				} else {
					SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");
					String regdate = fmt.format(doc.registration().getDate());
					String regnumber = doc.registration().getRegisteredInfo().getNumber().toString();
					String type = doc.getType();
					String crdate = fmt.format(doc.created().getTime().getTime());
					res = type + " от " + crdate
						+ (doc.getModule().getIdent() == "Input" ? (" (" + regdate + ") ") : " ")
						+ "№ " + regnumber;
				}
			}
			reasonTransfer.add(res);
		}
        return reasonTransfer;
    }

    private List<String> addOldDate (File templateFolder, String resid, String margin) {
        List<String> oldDate = new ArrayList();
        File sqlFile4 = new File(templateFolder, reasonTransferSqlFileName);
        String query4 = queryString1(sqlFile4, "$id", resid);
        IdentifiableObjectCollection collection4 = getCollection(query4);

        for (IdentifiableObject identifiableObject : collection4) {
			oldDate.add(identifiableObject.getString("olddatetable"));			
        }
        return oldDate;
    }

    private Map<String, String> fill(IdentifiableObject identifiableObject) {
        Map<String, String> map = new HashMap<>();
        String corrName = new String(identifiableObject.getString("corrName").getBytes(), StandardCharsets.UTF_8);
        corrName = corrName.equals("Не указан") ? corrName : identifiableObject.getString("corrName");
        map.put("«Корреспондент и Автор»", corrName
                + (identifiableObject.getString("authorName") != null ? (", " + identifiableObject.getString("authorName")) : ""));
        map.put("«Номер документа»", identifiableObject.getString("docNmber"));
        map.put("«дата документа»", identifiableObject.getString("docDate"));
        map.put("«Рег. номер»", identifiableObject.getString("regNumber"));
        map.put("«Дата регистрации»", identifiableObject.getString("regdate"));
        map.put("«Вид документа»", identifiableObject.getString("docType"));
        map.put("«Место регистрации»", identifiableObject.getString("regPlace"));
        map.put("«Заголовок документа»", identifiableObject.getString("subject"));
        map.put("«Адресаты»", identifiableObject.getString("addressees"));
        map.put("«Срок исполнения»", identifiableObject.getString("ctrldeadline"));

        if (identifiableObject.getString("control").equals("1")) {
            map.put("«Контроль»", "Контроль");
        } else {
            map.put("«Контроль»", "");
        }
        String rkkExecutionStatus = new String(identifiableObject.getString("rkkExecutionStatus").getBytes(), StandardCharsets.UTF_8);
        map.put("«Статус исполнения»", rkkExecutionStatus);
        if (identifiableObject.getString("rkkExecutionStatus").equals("Исполнен")) {
            map.put("«дата исполнения»", identifiableObject.getString("ctrldateexecution"));
            String whoexec = getWhoexec(identifiableObject);
            if (!whoexec.equals("")) {
                map.put("«автор отметки»", "(" + whoexec + ")");
            } else {
                map.put("«автор отметки»", "");
            }
        } else {
            map.put("«дата исполнения»", "");
            map.put("«автор отметки»", "");
        }

        return map;
    }

    private String getWhoexec(IdentifiableObject identifiableObject) {
        String name = "";
        String unid = identifiableObject.getString("docunid");
        try {
            Document doc = AFCMDomino.getDocByUNID(unid);
            String whoExecPID = doc.getItemValueString("WhoExecPID");
            String codeReplica = doc.getItemValueString("CodeReplica");
            String whoExecUnid = codeReplica + ":" + whoExecPID;

            if (whoExecUnid.split(":").length > 1) {
                Document whoExecDoc = AFCMDomino.getDocByUNID(whoExecUnid);
                name = whoExecDoc.getItemValueString("EShortName");
            }
        } catch (NotesException e) {
            throw new RuntimeException(e);
        }
        return name;
    }

    public void DotxTemplateFiller(File file, String outputPath, Map<String, String> map) {
        OutputStream out = null;
        try {
            XWPFDocument template = new XWPFDocument(new FileInputStream(file));

            List<XWPFParagraph> xwpfParagraphs = template.getParagraphs();
            replaceInParagraphs(xwpfParagraphs, map);

            List<XWPFTable> tables = template.getTables();
            for (XWPFTable xwpfTable : tables) {
                List<XWPFTableRow> tableRows = xwpfTable.getRows();
                for (XWPFTableRow xwpfTableRow : tableRows) {
                    List<XWPFTableCell> tableCells = xwpfTableRow.getTableCells();
                    for (XWPFTableCell xwpfTableCell : tableCells) {
                        xwpfParagraphs = xwpfTableCell.getParagraphs();
                        replaceInParagraphs(xwpfParagraphs, map);
                    }
                }
            }

            out = new FileOutputStream(new File(outputPath));
            template.write(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void replaceInParagraphs(List<XWPFParagraph> xwpfParagraphs, Map<String, String> map) {

        for (XWPFParagraph xwpfParagraph : xwpfParagraphs) {
            List<XWPFRun> xwpfRuns = xwpfParagraph.getRuns();
            int count = 0;
            for (XWPFRun xwpfRun : xwpfRuns) {
                String xwpfRunText = xwpfRun.getText(xwpfRun.getTextPosition());
                count++;

                if (StringUtils.isBlank(xwpfRunText) || map.get(xwpfRunText) == null || !(map.containsKey(xwpfRunText))) {
                    continue;
                }
                xwpfRunText = xwpfRunText.replace(xwpfRunText, map.get(xwpfRunText));

                xwpfRun.setText(xwpfRunText, 0);
            }
        }
    }

    private void getPositionParagrForDelete(File file, String resoltext, String filepath) throws Exception {

        XWPFDocument template = null;
        try {
            template = new XWPFDocument(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        XWPFParagraph xwpfParagraphDeleted = null;
        List<XWPFParagraph> xwpfParagraphs = new ArrayList();
        List<XWPFTable> tables = template.getTables();
        for (int j = 0; j < tables.size(); j++) {

            XWPFTable xwpfTable = tables.get(j);
            List<XWPFTableRow> tableRows = xwpfTable.getRows();

            for (int i = 0; i < tableRows.size(); i++) {
                XWPFTableRow xwpfTableRow = tableRows.get(i);
                List<XWPFTableCell> tableCells = xwpfTableRow.getTableCells();

                for (int t = 0; t < tableCells.size(); t++) {
                    XWPFTableCell xwpfTableCell = tableCells.get(t);
                    xwpfParagraphs = xwpfTableCell.getParagraphs();

                    for (XWPFParagraph xwpfParagraph : xwpfParagraphs) {
                        List<XWPFRun> xwpfRuns = xwpfParagraph.getRuns();
                        for (XWPFRun xwpfRun : xwpfRuns) {
                            String xwpfRunText = xwpfRun.getText(xwpfRun.getTextPosition());

                            if (StringUtils.isBlank(xwpfRunText) || !(xwpfRunText.contains("«Резолюция»"))) {
                                continue;
                            }

                            xwpfTableCell.removeParagraph(0);
                            XWPFParagraph paragraph = xwpfTableCell.addParagraph();

                            try {
                                HTMLtoDOCX(template, paragraph, resoltext, filepath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
	
	private void getPositionParagrForDeleteInTable(File file, List<String> texts1, List<String> texts2, String filepath) throws Exception {

        XWPFDocument template = null;
        try {
            template = new XWPFDocument(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        XWPFParagraph xwpfParagraphDeleted = null;
        List<XWPFParagraph> xwpfParagraphs = new ArrayList();
        List<XWPFTable> tables = template.getTables();

        XWPFTable xwpfTable = tables.get(1);
        List<XWPFTableRow> tableRows = xwpfTable.getRows();

        XWPFTableRow xwpfTableRow = tableRows.get(3);
        List<XWPFTableCell> tableCells = xwpfTableRow.getTableCells();

        XWPFTableCell cell = tableCells.get(0);
		
		List<IBodyElement> bodyElementsRef = getBodyElementsRef(cell);
		int startIndex = 0;
		int count = 2;
		if (texts1.isEmpty()) {
			for (int i = 0; i < count; i++) {
				IBodyElement element = bodyElementsRef.get(startIndex);
				if (element instanceof XWPFParagraph) {
					int realParIndex = getParagraphIndex(bodyElementsRef, (XWPFParagraph)element);
					cell.getParagraphs().remove(realParIndex);
					cell.getCTTc().removeP(realParIndex);
				} else if (element instanceof XWPFTable) {
					int realTableIndex = getTableIndex(bodyElementsRef, (XWPFTable) element);
					getTablesRef(cell).remove(realTableIndex);
					cell.getCTTc().removeTbl(realTableIndex);
				}
				bodyElementsRef.remove(startIndex);
			}
		} else {
			for(int i = 0; i < texts1.size(); i++) {
				String text1 = texts1.get(i) == "null" ? "" : texts1.get(i);
				String text2 = texts2.get(i) == "null" ? "" : texts2.get(i);
				XWPFTable table = cell.getTables().get(0);
				int size = table.getRows().size();
				XWPFTableRow r = table.createRow();
				r.getCell(0).removeParagraph(0);
				XWPFParagraph paragraph = r.getCell(0).addParagraph();
				setRun(paragraph.createRun(), "Times New Roman", 12, text1, false, false);
				
				r.getCell(1).removeParagraph(0);
				paragraph = r.getCell(1).addParagraph();
				setRun(paragraph.createRun(), "Times New Roman", 12, text2, false, false);
				paragraph.setAlignment(ParagraphAlignment.LEFT);
			}
		}
		savefile(template, filepath);
    }
	
	private static void setRun (XWPFRun run, String fontFamily, int fontSize , String text, boolean bold, boolean addBreak) {
        run.setFontFamily(fontFamily);
        run.setFontSize(fontSize);
        //run.setColor(colorRGB);
        run.setText(text);
        run.setBold(bold);
        if (addBreak) run.addBreak();
    }

	private int getParagraphIndex(List<IBodyElement> bodyElementsRef, XWPFParagraph paragraph) {
        int index = -1;
        for(IBodyElement elem : bodyElementsRef) {
            if (elem instanceof XWPFParagraph) {
                index++;
            }
            if (elem == paragraph) {
                return index;
            }
        }
        return -1;
    }

    private int getTableIndex(List<IBodyElement> bodyElementsRef, XWPFTable table) {
        int index = -1;
        for(IBodyElement elem : bodyElementsRef) {
            if (elem instanceof XWPFTable) {
                index++;
            }
            if (elem == table) {
                return index;
            }
        }
        return -1;
    }

    private List<XWPFTable> getTablesRef(XWPFTableCell cell) {
        try {
            Field beField = cell.getClass().getDeclaredField("tables");
            beField.setAccessible(true);
            return (List<XWPFTable>) beField.get(cell);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
	
	private List<IBodyElement> getBodyElementsRef(XWPFTableCell cell) {
        try {
            Field beField = cell.getClass().getDeclaredField("bodyElements");
            beField.setAccessible(true);
            return (List<IBodyElement>) beField.get(cell);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

	
	private void savefile(XWPFDocument destDoc, String filepath) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(filepath));
            destDoc.write(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override public String getFormat() {
        return ReportBuilderFormats.DOCX_FORMAT.getFormat();
    }

    private String getRequestId(Map<String, Object> parameters) {
        String request_id = parameters.get("request_id").toString();
        request_id = request_id.substring(request_id.indexOf("id=") + 3, request_id.length() - 1);
        return request_id;
    }

    public IdentifiableObjectCollection getCollection(String query) {
        try {
            return collectionsService.findCollectionByQuery(query);
        }catch (Exception ex) {
            throw new ReportServiceException("Error generate report query =" + query, ex);
        }
    }

    private String queryString1(File sqlFile, String old, String newv) {
        String query = "";
        try {
            query = FileUtils.readFileToString(sqlFile);
            query = query.replace(old, newv);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return query;
    }

    private String queryString4(File sqlFile, String old, String newv, String type, String newvtype) {
        String query = "";
        try {
            query = FileUtils.readFileToString(sqlFile);
            query = query.replace(old, newv);
            query = query.replace(type, newvtype);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return query;
    }
}