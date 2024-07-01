
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.dto.impl.RdbmsId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * Класс для получения строки c вопросами ОГ.
 */
public class ClCodeParser extends JRDefaultScriptlet {

    private final String REQUEST_QUESTION_QUERY_FORMAT = "SELECT idx, question FROM f_dp_requestrkk_question WHERE owner = %d ";
    private final String REQUEST_SUB_QUESTION_QUERY_FORMAT = "SELECT clcode, section, subject, theme, subquestion FROM f_dp_requestrkk_subquest WHERE owner = %d AND idx = %d ";

    /**
     * Получает строку с вопросами ОГ. Уровни разделяются символом " \ ",<br/>
     * а если вопросов больше 1го - они разделяются пустой строкой.
     *
     * @param connection объект соединения с БД
     * @param idObject   Id ОГ
     * @return полную строку с вопросами для вставки в документ.
     * @throws Exception
     */
    public String getRequestQuestions(Connection connection, Object idObject) throws Exception {

        RdbmsId rdbmsId = (RdbmsId) idObject;
        final long ownerId = rdbmsId.getId();

        String requestQuestionQuery = String.format(REQUEST_QUESTION_QUERY_FORMAT, ownerId);
        PreparedStatement requestQuestionStatement = connection.prepareStatement(requestQuestionQuery);

        List<QuestionResultData> resultDatalist = new LinkedList<>();
        ResultSet requestQuestionResultSet = requestQuestionStatement.executeQuery();

        while (requestQuestionResultSet.next()) {

            final int orderIndex = requestQuestionResultSet.getInt("idx");
            String question = requestQuestionResultSet.getString("question");

            final QuestionResultData questionResultData = new QuestionResultData();
            questionResultData.setOrderIndex(orderIndex);
            questionResultData.setQuestion(question);

            String requestSubQuestionQuery = String.format(REQUEST_SUB_QUESTION_QUERY_FORMAT, ownerId, orderIndex);
            PreparedStatement requestSubQuestionStatement = connection.prepareStatement(requestSubQuestionQuery);

            ResultSet requestSubQuestionResultSet = requestSubQuestionStatement.executeQuery();
            while (requestSubQuestionResultSet.next()) {

                fillQuestionResultData(questionResultData, requestSubQuestionResultSet);
                resultDatalist.add(questionResultData);
            }
            requestSubQuestionStatement.close();
        }
        requestQuestionStatement.close();

        final String fullThemeString = getFullThemeString(resultDatalist);
        return fullThemeString;
    }

    /**
     * Заполняет объект с данными вопроса: <br/>
     * все уровни, кроме 4го + полный код вопроса (остальное заполняется ранее).
     *
     * @param questionResultData          объект для заполнения
     * @param requestSubQuestionResultSet {@link ResultSet}, откуда будут взяты данные.
     * @throws SQLException
     */
    private void fillQuestionResultData(QuestionResultData questionResultData, ResultSet requestSubQuestionResultSet) throws SQLException {
        String clCode = requestSubQuestionResultSet.getString("clcode");
        questionResultData.setClCode(clCode);

        String section = requestSubQuestionResultSet.getString("section");
        questionResultData.setSection(section);

        String subject = requestSubQuestionResultSet.getString("subject");
        questionResultData.setSubject(subject);

        String theme = requestSubQuestionResultSet.getString("theme");
        questionResultData.setTheme(theme);

        String subquestion = requestSubQuestionResultSet.getString("subquestion");
        questionResultData.setSubquestion(subquestion);
    }

    /**
     * Создает и возвращает строку вопроса путем конкатенации всех уровней через символ " \ ".<br/>
     * Если вопросов больше 1го - между ними добавляется пустая строка.
     *
     * @param questionResultDataList список объектов с данными по каждому вопросу.
     * @return полную строку с вопросами для вставки в документ.
     */
    private String getFullThemeString(List<QuestionResultData> questionResultDataList) {
        sortResultData(questionResultDataList);
        String fullTheme = "";

        for (QuestionResultData resultData : questionResultDataList) {
            String clCode = resultData.getClCode();
            String question = resultData.getQuestion();

            String resDataTheme = clCode + "  " + question;

            if (fullTheme.equals("")) {
                fullTheme = resDataTheme;
            } else {
                fullTheme += "\n\n" + resDataTheme;
            }

            final boolean is5levelQuestion = is5levelQuestion(clCode);

            if (is5levelQuestion) {
                final String subquestion = resultData.getSubquestion();
                fullTheme += " \\ " + subquestion;
            }
        }
        return fullTheme;
    }

    /**
     * Определяет является ли вопрос 5ти уровневым.<br/>
     * Если нет - значит он 4х уровневый, так как иных пока не существует.
     *
     * @param clCode полная строка кодов каждого уровня, разделенных точкой (таким образом они хранятся в таблицах данных вопросов ОГ)
     * @return true - вопрос 5ти уровневый, false - нет
     */
    private boolean is5levelQuestion(String clCode) {
        final String[] codes = clCode.split("\\.");
        final int codesCount = codes.length;

        if (codesCount == 5) {
            return true;
        }
        return false;
    }

    /**
     * Сортирует объекты вопросов ОГ по тому, в каком порядке они были добавлены.
     *
     * @param questionResultDataList список с объектами данных вопросов ОГ.
     */
    private void sortResultData(List<QuestionResultData> questionResultDataList) {
        Collections.sort(questionResultDataList, new Comparator<QuestionResultData>() {
            @Override
            public int compare(QuestionResultData qrd1, QuestionResultData qrd2) {
                final Integer orderIndex1 = qrd1.getOrderIndex();
                final Integer orderIndex2 = qrd2.getOrderIndex();

                final int compareToResult = orderIndex1.compareTo(orderIndex2);
                return compareToResult;
            }
        });
    }

    /**
     * Объект данных вопроса ОГ.
     */
    private class QuestionResultData {
        /**
         * индекс (порядок добавления вопроса в ОГ)
         */
        private Integer orderIndex;
        /**
         * Вопрос (4ый уровень)
         */
        private String question;
        /**
         * Полный код : коды каждого уровня, разделенные точкой.
         */
        private String clCode;
        /**
         * Секция (1ый уровень)
         */
        private String section;
        /**
         * Предмет (2ой уровень)
         */
        private String subject;
        /**
         * Тема (3ий уровень)
         */
        private String theme;
        /**
         * Подвопрос (5 уровень)
         */
        private String subquestion;

        public Integer getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getClCode() {
            return clCode;
        }

        public void setClCode(String clCode) {
            this.clCode = clCode;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getSubquestion() {
            return subquestion;
        }

        public void setSubquestion(String subquestion) {
            this.subquestion = subquestion;
        }
    }

}