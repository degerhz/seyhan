/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.chqbll.reports;

import static play.data.Form.form;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import models.Bank;
import models.ChqbllType;
import models.Contact;
import models.GlobalPrivateCode;
import models.GlobalTransPoint;
import play.data.Form;
import play.data.format.Formats.DateTime;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import reports.ReportParams;
import reports.ReportService;
import reports.ReportService.ReportResult;
import utils.AuthManager;
import utils.CacheUtils;
import utils.DateUtils;
import utils.InstantSQL;
import views.html.chqblls.reports.chqbll_list;
import controllers.global.Profiles;
import enums.ChqbllSort;
import enums.ChqbllStep;
import enums.ReportUnit;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class ChqbllList extends Controller {

	private final static String REPORT_NAME = "ChqbllList";
	private final static Form<ChqbllList.Parameter> parameterForm = form(ChqbllList.Parameter.class);

	public static class Parameter {

		public ChqbllSort sort;
		public Boolean isCustomer;

		public String orderby;
		public String orderdir;
		public ReportUnit unit;

		public ChqbllStep lastStep;

		public Contact contact;
		public Bank bank;

		public GlobalTransPoint transPoint = Profiles.chosen().gnel_transPoint;
		public GlobalPrivateCode privateCode = Profiles.chosen().gnel_privateCode;

		@DateTime(pattern = "dd/MM/yyyy")
		public Date startDate = DateUtils.getFirstDayOfYear();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date endDate = new Date();

		public ChqbllType cbtype;

		public String reportType;

		public static Map<String, String> options() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			options.put("due_date", Messages.get("maturity"));
			options.put("last_step", Messages.get("last_status"));
			options.put("last_contact_name", Messages.get("contact.name"));
			options.put("bank_name", Messages.get("bank.name"));
			options.put("portfolio_no", Messages.get("portfolio.no"));
			options.put("serial_no", Messages.get("serial.no"));

			return options;
		}

		public static Map<String, String> orderdirOptions() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			options.put(" desc", Messages.get("descending"));
			options.put(" asc", Messages.get("ascending"));

			return options;
		}

		public static Map<String, String> reportTypes() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();

			options.put("Monthly", Messages.get("report.type.month_based"));
			options.put("Contact", Messages.get("report.type.contact_based"));
			options.put("Bank", Messages.get("report.type.bank_based"));
			options.put("BankAccount", Messages.get("report.type.bank_account_based"));
			options.put("PaymentPlace", Messages.get("report.type.payment_place_based"));
			options.put("LastStep", Messages.get("report.type.last_status_based"));
			options.put("Type", Messages.get("report.type.type_based"));
			options.put("ExcCode", Messages.get("report.type.exc_based"));
			options.put("Yearly", Messages.get("report.type.year_based"));
			options.put("Daily", Messages.get("report.type.day_based"));
			options.put("TransSource", Messages.get("report.type.trans_source_based"));

			return options;
		}

		public static Map<String, String> belonging() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();

			options.put(Boolean.TRUE.toString(),  Messages.get("enum.cqbl.Customer"));
			options.put(Boolean.FALSE.toString(), Messages.get("enum.cqbl.Firm"));

			return options;
		}

	}

	private static String getQueryString(Parameter params) {
		StringBuilder queryBuilder = new StringBuilder("");

		queryBuilder.append(" and t.workspace = " + CacheUtils.getWorkspaceId());

		queryBuilder.append(" and t.sort = '");
		queryBuilder.append(params.sort);
		queryBuilder.append("'");

		queryBuilder.append(" and is_customer = ");
		queryBuilder.append(params.isCustomer);

		if (params.lastStep != null) {
			queryBuilder.append(" and last_step = '");
			queryBuilder.append(params.lastStep.name());
			queryBuilder.append("'");
		}

		if (params.contact != null && params.contact.id != null) {
			queryBuilder.append(" and contact_id = ");
			queryBuilder.append(params.contact.id);
		}

		if (params.bank != null && params.bank.id != null) {
			queryBuilder.append(" and bank_id = ");
			queryBuilder.append(params.bank.id);
		}

		if (params.startDate != null) {
			queryBuilder.append(" and due_date >= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.startDate));
		}

		if (params.endDate != null) {
			queryBuilder.append(" and due_date <= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.endDate));
		}

		if (params.cbtype != null && params.cbtype.id != null) {
			queryBuilder.append(" and cbtype_id = ");
			queryBuilder.append(params.cbtype.id);
		}

		return queryBuilder.toString();
	}

	public static Result index(String sortStr) {
		ChqbllSort sort = ChqbllSort.Cheque;
		try {
			sort = ChqbllSort.valueOf(sortStr);
		} catch (Exception e) { }

		if (ChqbllSort.Cheque.equals(sort))
			return index(Right.CEK_LISTESI, sort);
		else
			return index(Right.SENET_LISTESI, sort);
	}

	public static Result generate(String sortStr) {
		ChqbllSort sort = ChqbllSort.Cheque;
		try {
			sort = ChqbllSort.valueOf(sortStr);
		} catch (Exception e) { }

		if (ChqbllSort.Cheque.equals(sort))
			return generate(Right.CEK_LISTESI, sort);
		else
			return generate(Right.SENET_LISTESI, sort);
	}

	private static Result generate(Right right, ChqbllSort sort) {
		Result hasProblem = AuthManager.hasProblem(right, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		Form<ChqbllList.Parameter> filledForm = parameterForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(chqbll_list.render(filledForm, sort));
		} else {

			Parameter params = filledForm.get();

			ReportParams repPar = new ReportParams();
			repPar.modul = "chqbll";
			repPar.reportName = REPORT_NAME;
			repPar.reportUnit = params.unit;
			repPar.query = getQueryString(params);
			repPar.orderBy = params.orderby + params.orderdir;

			repPar.paramMap.put("SORT",  params.sort.name());

			if (params.reportType != null && ! params.reportType.isEmpty()) {
				repPar.reportName = REPORT_NAME + "XBased";

				String field = "";
				String label = "";
				String type = "String";

				if (params.reportType.equals("LastStep")) {
					field = "last_step";
					label = Messages.get("last_status");
					type = "Step";
				}
				if (params.reportType.equals("Contact")) {
					field = "last_contact_name";
					label = Messages.get("contact.name");
				}
				if (params.reportType.equals("Bank")) {
					field = "bank_name";
					label = Messages.get("bank.name");
				}
				if (params.reportType.equals("BankAccount")) {
					field = "bank_account_no";
					label = Messages.get("bank.account");
				}
				if (params.reportType.equals("PaymentPlace")) {
					field = "payment_place";
					label = Messages.get("payment_place");
				}
				if (params.reportType.equals("Type")) {
					field = "cb.name";
					label = Messages.get("type");
				}
				if (params.reportType.equals("Monthly")) {
					field = "due_month";
					label = Messages.get("trans.month");
				}
				if (params.reportType.equals("Yearly")) {
					field = "due_year";
					label = Messages.get("trans.year");
					type = "Integer";
				}
				if (params.reportType.equals("Daily")) {
					field = "due_date";
					label = Messages.get("date");
					type = "Date";
				}
				if (params.reportType.equals("ExcCode")) {
					field = "exc_code";
					label = Messages.get("currency");
				}
				if (params.reportType.equals("TransSource")) {
					field = "ts.name";
					label = Messages.get("trans.source");
				}

				repPar.orderBy = field + ", " + params.orderby + params.orderdir;

				repPar.paramMap.put("GROUP_FIELD", field);
				repPar.paramMap.put("GROUP_LABEL", label);
				repPar.paramMap.put("GROUP_TYPE",  type);
			}

			/*
			 * Parametrik degerlerin gecisi
			 */
			repPar.paramMap.put("TRANS_POINT_SQL", "");
			if (params.transPoint != null && params.transPoint.id != null) {
				repPar.paramMap.put("TRANS_POINT_SQL", InstantSQL.buildTransPointSQL(params.transPoint.id));
			}

			repPar.paramMap.put("PRIVATE_CODE_SQL", "");
			if (params.privateCode != null && params.privateCode.id != null) {
				repPar.paramMap.put("PRIVATE_CODE_SQL", InstantSQL.buildPrivateCodeSQL(params.privateCode.id));
			}

			String par1 = Messages.get("chqbll.ofs", Messages.get(params.isCustomer ? "enum.cqbl.Customer" : "enum.cqbl.Firm"),  Messages.get(params.sort.key));
			String par2 = "(" + DateUtils.formatDateStandart(params.startDate) + " - " + DateUtils.formatDateStandart(params.endDate) +")";
			repPar.paramMap.put("REPORT_INFO", par1 + " - " + par2);

			ReportResult repRes = ReportService.generateReport(repPar, response());
			if (repRes.error != null) {
				flash("warning", repRes.error);
				return ok(chqbll_list.render(filledForm, sort));
			} else {
				return ok(repRes.stream);
			}
		}

	}

	private static Result index(Right right, ChqbllSort sort) {
		Result hasProblem = AuthManager.hasProblem(right, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(chqbll_list.render(parameterForm.fill(new Parameter()), sort));
	}

}
