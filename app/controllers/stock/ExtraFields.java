/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.stock;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import meta.GridHeader;
import meta.PageExtend;
import models.AdminExtraFields;
import models.StockExtraFields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AuthManager;
import utils.CacheUtils;
import utils.StringUtils;
import views.html.stocks.extra_fields.form;
import views.html.stocks.extra_fields.index;
import views.html.stocks.extra_fields.list;
import controllers.Application;
import controllers.global.Profiles;
import enums.Module;
import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class ExtraFields extends Controller {

	private final static Right RIGHT_SCOPE = Right.STOK_EKSTRA_ALANLAR;

	private final static Logger log = LoggerFactory.getLogger(ExtraFields.class);
	private final static Form<StockExtraFields> dataForm = form(StockExtraFields.class);

	private static String lastSaved;

	/**
	 * Liste formu basliklarini doner
	 * 
	 * @return List<GridHeader>
	 */
	private static List<GridHeader> getHeaderList() {
		List<GridHeader> headerList = new ArrayList<GridHeader>();
		headerList.add(new GridHeader(Messages.get("name"), true).sortable("name"));
		headerList.add(new GridHeader(Messages.get("is_active"), "7%", true));

		return headerList;
	}

	/**
	 * Liste formunda gosterilecek verileri doner
	 * 
	 * @return PageExtend
	 */
	private static PageExtend<StockExtraFields> buildPage(AdminExtraFields extraFields) {
		List<Map<Integer, String>> dataList = new ArrayList<Map<Integer, String>>();

		List<StockExtraFields> modelList = StockExtraFields.page(extraFields);
		if (modelList != null && modelList.size() > 0) {
			for (StockExtraFields model : modelList) {
				Map<Integer, String> dataMap = new HashMap<Integer, String>();
				int i = -1;
				dataMap.put(i++, model.id.toString());
				dataMap.put(i++, model.name);
				dataMap.put(i++, model.isActive.toString());

				dataList.add(dataMap);
			}
		}

		return new PageExtend<StockExtraFields>(getHeaderList(), dataList, null);
	}

	public static Result index(Integer extraFieldsId) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		AdminExtraFields extraFields = AdminExtraFields.findById(Module.stock.name(), extraFieldsId);
		return ok(
			index.render(buildPage(extraFields), extraFields)
		);
	}

	public static Result options(Integer extraFieldsId) {
		Result result = ok(StringUtils.buildOptionTag(StockExtraFields.options(extraFieldsId), lastSaved));
		lastSaved = null;

		return result;
	}

	/**
	 * Uzerinde veri bulunan liste formunu doner
	 */
	public static Result list(Integer extraFieldsId) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		AdminExtraFields extraFields = AdminExtraFields.findById(Module.stock.name(), extraFieldsId);
		return ok(
			list.render(buildPage(extraFields), extraFields)
		);
	}

	/**
	 * Kayit formundaki bilgileri kaydeder
	 */
	public static Result save() {
		if (! CacheUtils.isLoggedIn()) return Application.login();

		Form<StockExtraFields> filledForm = dataForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(form.render(filledForm));
		} else {

			StockExtraFields model = filledForm.get();

			Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, (model.id == null ? RightLevel.Insert : RightLevel.Update));
			if (hasProblem != null) return hasProblem;

			String editingConstraintError = model.checkEditingConstraints();
			if (editingConstraintError != null) return badRequest(editingConstraintError);

			checkConstraints(filledForm);

			if (filledForm.hasErrors()) {
				return badRequest(form.render(filledForm));
			}

			try {
				if (model.id == null) {
					model.save();
				} else {
					model.update();
				}
			} catch (OptimisticLockException e) {
				flash("error", Messages.get("exception.optimistic.lock"));
				return badRequest(form.render(dataForm.fill(model)));
			}
			lastSaved = model.name;

			flash("success", Messages.get("saved", model.name));
			if (Profiles.chosen().gnel_continuouslyRecording)
				return create(model.id);
			else
				return ok();
		}
	}

	/**
	 * Yeni bir kayit formu olusturur
	 */
	public static Result create(Integer extraFieldsId) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Insert);
		if (hasProblem != null) return hasProblem;

		StockExtraFields neu = new StockExtraFields();
		neu.extraFields = AdminExtraFields.findById(Module.stock.name(), extraFieldsId);

		return ok(form.render(dataForm.fill(neu)));
	}

	/**
	 * Secilen kayit icin duzenleme formunu acar
	 * 
	 * @param id
	 */
	public static Result edit(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			StockExtraFields model = StockExtraFields.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found"));
			} else {
				return ok(form.render(dataForm.fill(model)));
			}
		}
	}

	/**
	 * Duzenlemek icin acilmis olan kaydi siler
	 * 
	 * @param id
	 */
	public static Result remove(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Delete);
		if (hasProblem != null) return hasProblem;

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			StockExtraFields model = StockExtraFields.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found"));
			} else {
				String editingConstraintError = model.checkEditingConstraints();
				if (editingConstraintError != null) return badRequest(editingConstraintError);
				try {
					model.delete();
					flash("success", Messages.get("deleted", model.name));
					return ok();
				} catch (PersistenceException pe) {
					log.error("ERROR", pe);
					flash("error", Messages.get("delete.violation", model.name));
					return badRequest(Messages.get("delete.violation", model.name));
				}
			}
		}
	}

	/**
	 * Kayit isleminden once form uzerinde bulunan verilerin uygunlugunu kontrol eder
	 * 
	 * @param filledForm
	 */
	private static void checkConstraints(Form<StockExtraFields> filledForm) {
		StockExtraFields model = filledForm.get();

		if (StockExtraFields.isUsedForElse("name", model.name, model.id, model.extraFields)) {
			filledForm.reject("name", Messages.get("not.unique", model.name));
		}
	}

}
