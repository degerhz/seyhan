/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.stock.reports;

import static play.data.Form.form;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.AdminExtraFields;
import models.Contact;
import models.GlobalPrivateCode;
import models.GlobalTransPoint;
import models.SaleSeller;
import models.StockDepot;
import models.StockTransSource;
import models.temporal.ExtraFieldsForStock;
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
import utils.GlobalCons;
import utils.InstantSQL;
import utils.QueryUtils;
import views.html.stocks.reports.cumulative_report;
import controllers.global.Profiles;
import enums.Module;
import enums.ReportUnit;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class CumulativeReport extends Controller {

	private final static Right RIGHT_SCOPE = Right.STOK_ICMAL_RAPORU;
	private final static String REPORT_NAME = "CumulativeReportXBased";

	private final static Form<CumulativeReport.Parameter> parameterForm = form(CumulativeReport.Parameter.class);

	public static class Parameter extends ExtraFieldsForStock {

		public String orderBy;
		public ReportUnit unit;

		/*****************************************/

		public Contact contact;

		public GlobalTransPoint transPoint = Profiles.chosen().gnel_transPoint;
		public GlobalPrivateCode privateCode = Profiles.chosen().gnel_privateCode;

		public Right transType;
		public String transNo;
		public StockTransSource transSource;

		@DateTime(pattern = "dd/MM/yyyy")
		public Date startDate = DateUtils.getFirstDayOfMonth();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date endDate = new Date();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date deliveryDate;

		public StockDepot depot;
		public SaleSeller seller;

		public String reportType;

		public static Map<String, String> reportTypes() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();

			options.put("Stock", Messages.get("report.type.stock_based"));
			options.put("Monthly", Messages.get("report.type.month_based"));
			options.put("Yearly", Messages.get("report.type.year_based"));
			options.put("TransSource", Messages.get("report.type.trans_source_based"));
			options.put("ReceiptType", Messages.get("report.type.receipt_type_based"));
			options.put("Contact", Messages.get("report.type.contact_based"));
			options.put("Seller", Messages.get("report.type.seller_based"));
			options.put("Depot", Messages.get("report.type.depot_based"));

			List<AdminExtraFields> extraFieldList = AdminExtraFields.listAll(Module.stock.name());
			for (AdminExtraFields ef : extraFieldList) {
				options.put("ExtraFields"+ef.idno, Messages.get("report.type.x_based", ef.name));
			}

			return options;
		}

	}

	private static String getQueryString(Parameter params) {
		StringBuilder queryBuilder = new StringBuilder(" and s.is_active = " + GlobalCons.TRUE);

		queryBuilder.append(" and t.workspace = " + CacheUtils.getWorkspaceId());

		if (params.stock != null && params.stock.id != null) {
			queryBuilder.append(" and stock_id = ");
			queryBuilder.append(params.stock.id);
		}

		if (params.providerCode != null && ! params.providerCode.isEmpty()) {
			queryBuilder.append(" and s.providerCode = '");
			queryBuilder.append(params.providerCode);
			queryBuilder.append("'");
		}

		QueryUtils.addExtraFieldsCriterias(params, queryBuilder, "s.");

		/************************************************************************/

		if (params.contact != null && params.contact.id != null) {
			queryBuilder.append(" and contact_id = ");
			queryBuilder.append(params.contact.id);
		}

		if (params.startDate != null) {
			queryBuilder.append(" and trans_date >= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.startDate));
		}

		if (params.endDate != null) {
			queryBuilder.append(" and trans_date <= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.endDate));
		}

		if (params.deliveryDate != null) {
			queryBuilder.append(" and delivery_date = ");
			queryBuilder.append(DateUtils.formatDateForDB(params.deliveryDate));
		}

		if (params.depot != null && params.depot.id != null) {
			queryBuilder.append(" and depot_id = ");
			queryBuilder.append(params.depot.id);
		}

		if (params.seller != null && params.seller.id != null) {
			queryBuilder.append(" and seller_id = ");
			queryBuilder.append(params.seller.id);
		}

		if (params.transType != null) {
			queryBuilder.append(" and _right = '");
			queryBuilder.append(params.transType);
			queryBuilder.append("'");
		}

		if (params.transNo != null && ! params.transNo.isEmpty()) {
			queryBuilder.append(" and trans_no = '");
			queryBuilder.append(params.transNo);
			queryBuilder.append("'");
		}

		if (params.transSource != null && params.transSource.id != null) {
			queryBuilder.append(" and trans_source_id = ");
			queryBuilder.append(params.transSource.id);
		}

		return queryBuilder.toString();
	}

	public static Result generate() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		Form<CumulativeReport.Parameter> filledForm = parameterForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(cumulative_report.render(filledForm));
		} else {

			Parameter params = filledForm.get();

			ReportParams repPar = new ReportParams();
			repPar.modul = RIGHT_SCOPE.module.name();
			repPar.reportName = REPORT_NAME;
			repPar.reportNameExtra = REPORT_NAME;
			repPar.query = getQueryString(params);
			repPar.reportUnit = params.unit;

			String field = "";
			String label = "";

			if (params.reportType.equals("Stock")) {
				repPar.reportName = "CumulativeReportStockBased";
			} else {
				if (params.reportType.equals("Contact")) {
					field = "co.name";
					label = Messages.get("contact.name");
				}
				if (params.reportType.equals("Seller")) {
					field = "sel.name";
					label = Messages.get("seller");
				}
				if (params.reportType.equals("Depot")) {
					field = "d.name";
					label = Messages.get("depot");
				}
				if (params.reportType.equals("Monthly")) {
					field = "t.trans_month";
					label = Messages.get("trans.month");
				}
				if (params.reportType.equals("Yearly")) {
					field = "t.trans_year";
					label = Messages.get("trans.year");
				}
				if (params.reportType.equals("ReceiptType")) {
					field = "t._right";
					label = Messages.get("trans.type");
				}
				if (params.reportType.equals("TransSource")) {
					field = "ts.name";
					label = Messages.get("trans.source");
				}
				if (params.reportType.startsWith("ExtraFields")) {
					Integer extraFieldsId = Integer.parseInt(""+params.reportType.charAt(params.reportType.length()-1));
					field = "ef"+extraFieldsId+".name";
					AdminExtraFields aef = AdminExtraFields.findById(Module.stock.name(), extraFieldsId);
					label = aef.name;
				}

				repPar.paramMap.put("GROUP_FIELD", field);
				repPar.paramMap.put("GROUP_LABEL", label);
			}
			/*
			 * Parametrik degerlerin gecisi
			 */
			repPar.paramMap.put("EXTRA_FIELDS_SQL", QueryUtils.buildExtraFieldsQueryForStock(params, field));

			repPar.paramMap.put("CATEGORY_SQL", "");
			if (params.category != null && params.category.id != null) {
				repPar.paramMap.put("CATEGORY_SQL", InstantSQL.buildCategorySQL(params.category.id));
			}

			repPar.paramMap.put("TRANS_POINT_SQL", "");
			if (params.transPoint != null && params.transPoint.id != null) {
				repPar.paramMap.put("TRANS_POINT_SQL", InstantSQL.buildTransPointSQL(params.transPoint.id));
			}

			repPar.paramMap.put("PRIVATE_CODE_SQL", "");
			if (params.privateCode != null && params.privateCode.id != null) {
				repPar.paramMap.put("PRIVATE_CODE_SQL", InstantSQL.buildPrivateCodeSQL(params.privateCode.id));
			}

			String par1 = Parameter.reportTypes().get(params.reportType);
			String par2 = "(" + DateUtils.formatDateStandart(params.startDate) + " - " + DateUtils.formatDateStandart(params.endDate) +")";
			repPar.paramMap.put("REPORT_INFO", par1 + " - " + par2);

			ReportResult repRes = ReportService.generateReport(repPar, response());
			if (repRes.error != null) {
				flash("warning", repRes.error);
				return ok(cumulative_report.render(filledForm));
			} else {
				return ok(repRes.stream);
			}
		}

	}

	public static Result index() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(cumulative_report.render(parameterForm.fill(new Parameter())));
	}

}
