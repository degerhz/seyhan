/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.safe.reports;

import static play.data.Form.form;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import models.GlobalPrivateCode;
import models.GlobalTransPoint;
import models.Safe;
import models.SafeExpense;
import models.SafeTransSource;
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
import views.html.safes.reports.trans_list;
import controllers.global.Profiles;
import enums.ReportUnit;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class TransactionList extends Controller {

	private final static Right RIGHT_SCOPE = Right.KASA_ISLEM_LISTESI;
	private final static String REPORT_NAME = "TransactionList";

	private final static Form<TransactionList.Parameter> parameterForm = form(TransactionList.Parameter.class);

	public static class Parameter {

		public String orderBy;
		public ReportUnit unit;

		public Safe safe = Profiles.chosen().gnel_safe;
		public SafeExpense expense;

		public GlobalTransPoint transPoint = Profiles.chosen().gnel_transPoint;
		public GlobalPrivateCode privateCode = Profiles.chosen().gnel_privateCode;

		public Right transType;
		public String transNo;
		public SafeTransSource transSource;

		public String excCode;

		@DateTime(pattern = "dd/MM/yyyy")
		public Date startDate = DateUtils.getFirstDayOfMonth();

		@DateTime(pattern = "dd/MM/yyyy")
		public Date endDate = new Date();

		public static Map<String, String> options() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			options.put("t.trans_date", Messages.get("date"));
			options.put("t.trans_no", Messages.get("trans.no"));
			options.put("e.name", Messages.get("safe.expense"));

			return options;
		}

	}

	private static String getQueryString(Parameter params) {
		StringBuilder queryBuilder = new StringBuilder("");

		queryBuilder.append(" and t.workspace = " + CacheUtils.getWorkspaceId());

		if (params.safe != null && params.safe.id != null) {
			queryBuilder.append(" and safe_id = ");
			queryBuilder.append(params.safe.id);
		}

		if (params.expense != null && params.expense.id != null) {
			queryBuilder.append(" and expense_id = ");
			queryBuilder.append(params.expense.id);
		}

		if (params.startDate != null) {
			queryBuilder.append(" and trans_date >= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.startDate));
		}

		if (params.endDate != null) {
			queryBuilder.append(" and trans_date <= ");
			queryBuilder.append(DateUtils.formatDateForDB(params.endDate));
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

		if (params.excCode != null && ! params.excCode.isEmpty()) {
			queryBuilder.append(" and exc_code = '");
			queryBuilder.append(params.excCode);
			queryBuilder.append("'");
		}

		return queryBuilder.toString();
	}

	public static Result generate() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		Form<TransactionList.Parameter> filledForm = parameterForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(trans_list.render(filledForm));
		} else {

			Parameter params = filledForm.get();

			ReportParams repPar = new ReportParams();
			repPar.modul = RIGHT_SCOPE.module.name();
			repPar.reportName = REPORT_NAME;
			repPar.reportUnit = params.unit;
			repPar.query = getQueryString(params);
			repPar.orderBy = params.orderBy;

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
			repPar.paramMap.put("REPORT_INFO", Messages.get("report.info.date_range", DateUtils.formatDateStandart(params.startDate), DateUtils.formatDateStandart(params.endDate)));

			ReportResult repRes = ReportService.generateReport(repPar, response());
			if (repRes.error != null) {
				flash("warning", repRes.error);
				return ok(trans_list.render(filledForm));
			} else {
				return ok(repRes.stream);
			}
		}

	}

	public static Result index() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(trans_list.render(parameterForm.fill(new Parameter())));
	}

}
