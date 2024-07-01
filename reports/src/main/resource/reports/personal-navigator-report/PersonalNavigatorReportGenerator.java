
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.springframework.beans.factory.annotation.Autowired;
import ru.intertrust.cm.core.business.api.CollectionsService;
import ru.intertrust.cm.core.business.api.CrudService;
import ru.intertrust.cm.core.config.model.ReportMetadataConfig;
import ru.intertrust.cm.core.model.ReportServiceException;
import ru.intertrust.cm.core.service.api.ReportGenerator;
import ru.intertrust.cmj.af.collections.CLBuilder;
import ru.intertrust.cmj.af.collections.CLCollection;
import ru.intertrust.cmj.af.collections.impl.CanSetNavigatorUnid;
import ru.intertrust.cmj.af.collections.impl.Description;
import ru.intertrust.cmj.af.collections.impl.DescriptionCache;
import ru.intertrust.cmj.af.core.AFCMDomino;
import ru.intertrust.cmj.af.core.AFMessages;
import ru.intertrust.cmj.af.core.AFSession;
import ru.intertrust.cmj.af.so.SOApplication;
import ru.intertrust.cmj.af.so.SOPersonSystem;

import org.springframework.context.i18n.LocaleContextHolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class PersonalNavigatorReportGenerator implements ReportGenerator {

	private static final String PARAM_PERSON_DN = "personDN";
	private static final String PARAM_PERSON_FIO = "personFIO";
	private static final String PARAM_SOREPLICAID = "soReplicaId";
	private static final String PARAM_NAVIGATOR_ALIAS = "navigatorAlias";
	private static final String PARAM_NAVIGATOR_UNID = "navigatorUnid";

	@Autowired
	private CollectionsService collectionsService;
	@Autowired
	private CrudService crudService;

	@Override
	public InputStream generate(ReportMetadataConfig reportMetadata, File templateFolder, Map<String, Object> parameters) {
		try {

			String personDN = (String) parameters.get(PARAM_PERSON_DN);
			String replicaIdSO = (String) parameters.get(PARAM_SOREPLICAID);
			String navigatorAlias = (String) parameters.get(PARAM_NAVIGATOR_ALIAS);
			String navigatorUnid = (String) parameters.get(PARAM_NAVIGATOR_UNID);

			/* фиксируем локаль, так как колелкция (vw_cmj_nav) её требует*/
			final Locale loc_RU = new Locale("ru", "RU");
			LocaleContextHolder.setLocale(loc_RU);

			/* вычисление иерархии формируется от имени нужного пользователя */
			AFSession afSession = AFSession.get();
			if(!afSession.isRunAsSystem()) {
				afSession.runAsSystem();
			}
			AFCMDomino.setCurrentOrganization(replicaIdSO);

			SOApplication soApp = AFSession.get().getApplication(SOApplication.class);
			SOPersonSystem person = soApp.getPersonByDnAndSoDbReplicaId(personDN, replicaIdSO);
			afSession.setFakeUser(person);

			/* берем билдер для вычисления коллекци папок */
			String catalogReplicaId  = AFCMDomino.getCurrentEDMCatalogReplicaId(replicaIdSO);
			final Description dsc = DescriptionCache.getInstance().getDescription("af-web-navigator", replicaIdSO);
			if (dsc == null)
				throw new RuntimeException(
						AFMessages.get("cmj-AF.collections.UnableToGetCollectionDescription", "af-web-navigator"));
			CanSetNavigatorUnid builder = (CanSetNavigatorUnid) dsc.getBuilder(catalogReplicaId);
			//UiBuilderNavigator builder = (UiBuilderNavigator) afSession.getApplication(CLApplication.class).getBuilder("EDM-Catalog%", "af-web-navigator");
			//builder - синглтон. Необходимо его клонировать, иначе при последующем обращении к билдеру (например, при построении коллекции)
			//будем получать значения navigatorUNID и alias, которые были установлены при последнем вызове текущего метода (не обязательно из текущего потока).
			CanSetNavigatorUnid builderCopy = ((CanSetNavigatorUnid) builder).clone();
			if (builderCopy!=null) {
				builderCopy.setNavigatorUNID(navigatorUnid);
				builderCopy.setNavigatorName(navigatorAlias);
				builder = builderCopy;
			}

			/* формируем коллекцию со всей иерархией */
			String anchorString = "?";
			Date date = null;
			CLBuilder.CustomFilterAndSorting setting = null;
			CLCollection collection = ((CLBuilder) builder).getNext(anchorString, 1000, Collections.emptyList(), date, CLBuilder.HIERARCHY_OPERATION.EXPAND_ALL);
			String anchor = "";
			collectChildren((CLBuilder) builder, collection, anchor);

			/* формируем иерархию в текстовом виде */
			String htmlHierarchyString = "";
			String lineTab = "";
			StringBuilder htmlHierarchyStringBuilder = new StringBuilder();
			for (CLCollection.Entry entry : collection.entries()) {
				htmlHierarchyStringBuilder.append("<p>");
				htmlHierarchyStringBuilder.append(lineTab + getEntryTitle(entry));
				htmlHierarchyStringBuilder.append("</p>");
				processEntryChildren(entry, htmlHierarchyStringBuilder,lineTab + "    ");
			}
			String hierarchyAsHtml = htmlHierarchyStringBuilder.toString();
			String userName = person.name().getFull();
			String htmlString = getHtmlReport(hierarchyAsHtml, userName, navigatorAlias);

			/* формируется файл отчёта */
			File newHtmlFile = File.createTempFile("new", ".html");
			/*WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
			NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
			wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
			ndp.unmarshalDefaultNumbering();

			XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
			String baseURLNotUsed = "";
			System.out.println(htmlString);
			wordMLPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(htmlString, baseURLNotUsed));
			wordMLPackage.save(newHtmlFile);*/
			//из-за проблем с библиотеками docx4j вместо формирования docx-файла теперь формируем html-файл.
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newHtmlFile), "UTF-8"))) {
				writer.write(htmlString);
			}

			afSession.clearFakeUser();

			return new FileInputStream(newHtmlFile);
		} catch (Exception ex) {
			throw new ReportServiceException("Error generate report", ex);
		}
	}

	/**
	 * Собирает все дочерние папки и view для всех эелментов переданной коллекции
	 */
	public void collectChildren(CLBuilder builder, CLCollection collection, String anchor) {
		if (collection.entries().isEmpty()) return;
		for (CLCollection.Entry entry : collection.entries()) {
			if (entry != null &&  entry.type().name().equals("tab")) {
				String anchorChild = entry.anchor();
				CLCollection entryChildren = builder.getChilds(anchorChild, 100);
				collectChildren(builder, entryChildren, anchorChild);
				entry.setChilds(entryChildren);
			}
		}
	}

	/**
	 * Формирует иеррхию папок, дочерних для переданной entry в текстовом виде
	 */
	public void processEntryChildren(CLCollection.Entry entry, StringBuilder htmlHierarchyStringBuilder, String lineTab) {
		if (entry == null) return;
		if (!entry.hasChilds()) return;
		if (entry.getChilds() == null) return;
		if (entry.getChilds().entries() == null) return;

		for (CLCollection.Entry child : entry.getChilds().entries()) {
			htmlHierarchyStringBuilder.append("<pre>");
			htmlHierarchyStringBuilder.append(lineTab + getEntryTitle(child));
			htmlHierarchyStringBuilder.append("</pre>");

			processEntryChildren(child, htmlHierarchyStringBuilder,lineTab + "    ");
		}
	}

	/**
	 * Вычисляет название папки
	 */
	public String getEntryTitle(CLCollection.Entry entry) {
		String title = "";
		Object value = entry.value();
		if (value instanceof Map) {
			Vector vectorTitle = (Vector) ((Map) value).get("title");
			title = (String) vectorTitle.get(0);
		}
		return title;
	}

	//формирование html-страницы
	private String getHtmlReport(String hierarchyAsHtml, String userName, String navigatorName){
		String htmlString = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN\" \"http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><!--This file was converted to xhtml by LibreOffice - see https://cgit.freedesktop.org/libreoffice/core/tree/filter/source/xslt for the code.--><head profile=\"http://dublincore.org/documents/dcmi-terms/\"><meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=utf-8\"/><title xml:lang=\"en-US\">- no title specified</title><meta name=\"DCTERMS.title\" content=\"\" xml:lang=\"en-US\"/><meta name=\"DCTERMS.language\" content=\"en-US\" scheme=\"DCTERMS.RFC4646\"/><meta name=\"DCTERMS.source\" content=\"http://xml.openoffice.org/odf2xhtml\"/><meta name=\"DCTERMS.creator\" content=\"Evgeniya\"/><meta name=\"DCTERMS.issued\" content=\"2021-06-10T06:34:00\" scheme=\"DCTERMS.W3CDTF\"/><meta name=\"DCTERMS.modified\" content=\"2021-08-24T18:45:31.800000000\" scheme=\"DCTERMS.W3CDTF\"/><meta name=\"DCTERMS.provenance\" content=\"\" xml:lang=\"en-US\"/><meta name=\"DCTERMS.subject\" content=\",\" xml:lang=\"en-US\"/><link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" hreflang=\"en\"/><link rel=\"schema.DCTERMS\" href=\"http://purl.org/dc/terms/\" hreflang=\"en\"/><link rel=\"schema.DCTYPE\" href=\"http://purl.org/dc/dcmitype/\" hreflang=\"en\"/><link rel=\"schema.DCAM\" href=\"http://purl.org/dc/dcam/\" hreflang=\"en\"/><style type=\"text/css\">\n" +
				"    @page {  }\n" +
				"	 * {font-family:Arial;" +
				"	 	background-color:transparent; }" +
				"    td, th { vertical-align:top; font-size:12pt;}\n" +
				"    h1, h2, h3, h4, h5, h6 { clear:both;}\n" +
				"    ol, ul { margin:0; padding:0;}\n" +
				"    span.footnodeNumber { padding-right:1em; }\n" +
				"    span.annotation_style_by_filter { font-size:95%; background-color:#fff000;  margin:0; border:0; padding:0;  }\n" +
				"    span.heading_numbering { margin-right: 0.8rem; }" +
				"* { margin:0;}\n" +
				"    .Header { font-size:11pt; line-height:100%; margin-bottom:0cm; margin-top:0cm; text-align:left ! important; font-family:Calibri; writing-mode:horizontal-tb; direction:ltr; }\n" +
				"    .Strong { font-weight:bold; }\n" +
				"    </style></head>" +
				"<body dir=\"ltr\" style=\"max-width:21cm;margin-top:2cm; margin-bottom:2cm; margin-left:2.54cm; margin-right:2.54cm; \">\n" +
				"   <h1>Персональная иерархия папок </h1>\n " +
				"   <h1>пользователь: " + userName + "</h1>\n"  +
				"   <h1>навигатор: " + navigatorName + "</h1>\n" +
				"   <p> </p> <br>\n";

		htmlString = htmlString + hierarchyAsHtml;

		htmlString = htmlString
				+ "</body>\n"
				+ "</html>";

		return htmlString;
	}

	@Override
	public String getFormat(){
		return "HTML";
	}
}