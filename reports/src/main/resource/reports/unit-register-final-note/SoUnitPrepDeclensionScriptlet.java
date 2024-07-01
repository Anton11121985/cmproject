import net.sf.jasperreports.engine.JRDefaultScriptlet;

import ru.intertrust.cmj.admin.declension.impl.CaseDeclensionServiceFactory;
import ru.intertrust.cmj.admin.declension.CaseDeclensionService.Case;

public class SoUnitPrepDeclensionScriptlet extends JRDefaultScriptlet {

    /**
     * Получение склонения наименования Структурного подразделения в предложном падеже
     *
     * @param name - "наименование Структурного подразделения"
     */
    public String declineSoUnitToPrepCase(String name) {
        return CaseDeclensionServiceFactory.get().declineStructureUnitName(name).get(Case.PREPOSITIONAL);
    }
}