import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import ru.intertrust.cm.core.business.api.AttachmentService;
import ru.intertrust.cm.core.business.api.dto.DomainObject;
import ru.intertrust.cm.core.business.api.dto.Id;
import ru.intertrust.cmj.af.utils.BeansUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.springframework.beans.factory.annotation.Autowired;

public class AttachmentProjectDecisionScriptlet extends JRDefaultScriptlet {

	@Autowired
	private AttachmentService attachmentService;

	public String getAttachmentText(Object objDocId){
	    StringBuffer buffer = new StringBuffer();
	    if (objDocId == null) {
	        return buffer.toString();
	    }

		Id docId = (Id) objDocId;
		for(DomainObject o : attachmentService.findAttachmentDomainObjectsFor(docId, "F_ContentRT_DecPr")){
			if(buffer.length() > 0){
				buffer.append("\n");
			}
			try{
				buffer.append(convertToStirng(RemoteInputStreamClient.wrap(attachmentService.loadAttachment(o.getId()))));
			} catch (Exception e){
				throw new RuntimeException();
			}
		}
		return buffer.toString();
	}

	private String convertToStirng(InputStream inputStream) {
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder out = new StringBuilder();
		try (
				Reader in = new InputStreamReader(inputStream, "UTF-8"))

		{
			for (; ; ) {
				int rsz = in.read(buffer, 0, buffer.length);
				if (rsz < 0)
					break;
				out.append(buffer, 0, rsz);
			}
			return out.toString();


		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}