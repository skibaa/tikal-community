package tikalk;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.samples.petclinic.Clinic;
import org.springframework.samples.petclinic.Owner;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.appengine.api.xmpp.*;

public class XMPPServlet extends HttpServlet {
	ApplicationContext appContext;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		appContext=WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Clinic c=(Clinic) appContext.getBean("clinic", Clinic.class);
		resp.getWriter().print("XMPP servlet is alive");
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		XMPPService xmpp = XMPPServiceFactory.getXMPPService();
		Message message = xmpp.parseMessage(req);

		JID fromJid = message.getFromJid();
		String body = message.getBody();
		JID toJid =  message.getRecipientJids()[0];
		
		Clinic clinic=(Clinic) appContext.getBean("clinic", Clinic.class);
		Collection<Owner> owners=clinic.findOwners(body);
		
		MessageBuilder builder=new MessageBuilder();
		if (owners.size()==0) {
			xmpp.sendMessage(builder
				.withRecipientJids(fromJid)
				.withBody("No owners found")
				.withFromJid(toJid)
				.build()
			);
			return;
		}
		
		for (Owner owner: owners) {
			xmpp.sendMessage(builder
				.withRecipientJids(fromJid)
				.withBody("First Name: "+owner.getFirstName()
						+"\nLast Name: "+owner.getLastName()
						+"\nAddress: "+owner.getAddress())
				.withFromJid(toJid)
				.build()
			);
		}
	}
}
