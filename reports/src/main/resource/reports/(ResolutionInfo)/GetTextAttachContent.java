import com.healthmarketscience.rmiio.RemoteInputStreamClient;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.AttachmentService;
import ru.intertrust.cm.core.business.api.dto.DomainObject;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cm_sochi.srv.util.PlainTextExtractor;
import ru.intertrust.cmj.af.utils.BeansUtils;
import com.healthmarketscience.rmiio.RemoteInputStream;

import java.io.InputStream;

public class GetTextAttachContent extends JRDefaultScriptlet {

    public String getAttachmentText(Object docId) {
        StringBuffer buffer = new StringBuffer();
        AttachmentService attachmentService = BeansUtils.getBean("attachmentService");
        for (DomainObject o : attachmentService.findAttachmentDomainObjectsFor((Id) docId, "F_ContentRichText_Res")) {
            if (buffer.length() > 0) {
                buffer.append("\n");
            }
            try {
                Id id = o.getId();
                if (id != null) {
                    RemoteInputStream ris = attachmentService.loadAttachment(id);
                    if (ris != null) {
                        InputStream is = RemoteInputStreamClient.wrap(ris);
                        if (is != null) {
                            String str = PlainTextExtractor.extractPlainText(is);
                            buffer.append(str);
                        }
                    }
                }
            } catch (Exception e) {
                return "";
            }
        }
        return buffer.toString();
    }

}
