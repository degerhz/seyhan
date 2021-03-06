/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.global;

import static play.data.Form.form;

import java.util.List;

import javax.persistence.OptimisticLockException;

import models.GlobalTransPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.api.mvc.SimpleResult;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AuthManager;
import utils.CacheUtils;
import views.html.globals.trans_point.form;
import views.html.globals.trans_point.index;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlRow;

import enums.Right;
import enums.RightLevel;

/**
 * @author mdpinar
*/
public class TransPoints extends Controller {

	private final static Right RIGHT_SCOPE = Right.GNEL_ISLEM_NOKTALARI;

	private final static Logger log = LoggerFactory.getLogger(TransPoints.class);
	private final static Form<GlobalTransPoint> dataForm = form(GlobalTransPoint.class);

	public static Result index() {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) return hasProblem;

		return ok(index.render());
	}

	/**
	 * Uzerinde veri bulunan liste formunu doner
	 */
	public static Result list() {
		if (! CacheUtils.isLoggedIn()) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		return ok();
	}

	/**
	 * Kayit formundaki bilgileri kaydeder
	 */
	public static Result save() {
		if (! CacheUtils.isLoggedIn()) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		Form<GlobalTransPoint> filledForm = dataForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			return badRequest(form.render(filledForm));
		} else {

			GlobalTransPoint model = filledForm.get();

			Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, (model.id == null ? RightLevel.Insert : RightLevel.Update));
			if (hasProblem != null) return hasProblem;

			String editingConstraintError = model.checkEditingConstraints();
			if (editingConstraintError != null) return badRequest(editingConstraintError);

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

			return ok(model.id.toString());
		}
	}

	public static Result create(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Insert);
		if (hasProblem != null) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		if (id != null) {
			GlobalTransPoint model = GlobalTransPoint.findById(id);
			if (model != null) {
				if (model.par5Id != null) {
					return badRequest(Messages.get("limit.alert", Messages.get("trans.point"), 6));
				}
			}
		}

		return ok(form.render(dataForm.fill(new GlobalTransPoint(id))));
	}

	/**
	 * Secilen kayit icin duzenleme formunu acar
	 * 
	 * @param id
	 */
	public static Result edit(Integer id) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Enable);
		if (hasProblem != null) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			GlobalTransPoint model = GlobalTransPoint.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found", Messages.get("trans.point")));
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
		if (hasProblem != null) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		if (id == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {
			GlobalTransPoint model = GlobalTransPoint.findById(id);
			if (model == null) {
				return badRequest(Messages.get("not.found", Messages.get("trans.point")));
			} else {
				String editingConstraintError = model.checkEditingConstraints();
				if (editingConstraintError != null) return badRequest(editingConstraintError);
				try {
					String parId = "1";

					if (model.par1Id == null) 
						;
					else if (model.par2Id == null) 
						parId = "2";
					else if (model.par3Id == null) 
						parId = "3";
					else if (model.par4Id == null) 
						parId = "4";
					else if (model.par5Id == null) 
						parId = "5";

					Ebean.createSqlUpdate("delete from global_trans_point where id = :id or par" + parId + "Id = :parId ")
							.setParameter("id", id)
							.setParameter("parId", model.id)
						.execute();
					CacheUtils.cleanAll(GlobalTransPoint.class, Right.GNEL_ISLEM_NOKTALARI);

					return ok(Messages.get("deleted", model.name));
				} catch (Exception pe) {
					flash("error", Messages.get("delete.violation", model.name));
					log.error("ERROR", pe);
					return badRequest();
				}
			}
		}
	}

	public static Result paste(Integer sourceId, Integer targetId, Integer op) {
		Result hasProblem = AuthManager.hasProblem(RIGHT_SCOPE, RightLevel.Update);
		if (hasProblem != null) {
			return badRequest(Messages.get("not.authorized.or.disconnect"));
		}

		if (sourceId == null) {
			return badRequest(Messages.get("id.is.null"));
		} else {

			GlobalTransPoint source = GlobalTransPoint.findById(sourceId);
			GlobalTransPoint parent = GlobalTransPoint.findById(targetId);

			if (source == null || parent == null) {
				return badRequest(Messages.get("not.found", Messages.get("trans.point")));
			} else {

				if (parent.par5Id != null) {
					return badRequest(Messages.get("limit.alert", Messages.get("trans.point"), 6));
				}

				if (sourceId.equals(parent.par1Id)
				||  sourceId.equals(parent.par2Id)
				||  sourceId.equals(parent.par3Id)
				||  sourceId.equals(parent.par4Id)
				||  sourceId.equals(parent.par5Id)) { 
					return badRequest(Messages.get("category.sibling.alert"));
				}

				Ebean.beginTransaction();
				try {
					GlobalTransPoint target = null;
					if (op == 3) {
						target = parent;
					} else {
						target = new GlobalTransPoint(parent);
						target.name = source.name;
						target.save();
					}

					int level = 1;
					if (source.par1Id != null) level++;
					if (source.par2Id != null) level++;
					if (source.par3Id != null) level++;
					if (source.par4Id != null) level++;
					if (source.par5Id != null) level++;

					copy(sourceId, target.id, level, op);

					if (op != null && op.equals(9)) { //for cut op
						Result result = remove(sourceId);
						if (((SimpleResult)result.getWrappedResult()).header().status() != 200) {
							Ebean.rollbackTransaction();
							return result;
						}
					}
					Ebean.commitTransaction();
					return ok();

				} catch (Exception e) {
					Ebean.rollbackTransaction();
					log.error(e.getMessage(), e);
					return badRequest(e.getMessage());
				}
			}
		}
	}

	private static void copy(Integer sourceId, Integer targetId, int level, Integer op) {
		if (level > 5) return;

		StringBuilder query = new StringBuilder();

		query.append("select id from global_trans_point where par"+level+"id = :id ");
		for (int i = level + 1; i < 6; i++) {
			query.append(" and par"+i+"id is null ");
		}

		List<SqlRow> idList = Ebean.createSqlQuery(query.toString())
									.setParameter("id", sourceId)
									.findList();

		for (SqlRow row : idList) {
			Integer parentId = row.getInteger("id");

			GlobalTransPoint old = GlobalTransPoint.findById(parentId);
			GlobalTransPoint target = null;

			if (op != 2) {
				target = new GlobalTransPoint(targetId);
				target.name = old.name;
				target.save();
			} else {
				target = GlobalTransPoint.findById(targetId);
			}
			copy(old.id, target.id, ++level, op);
			level--;
		}

	}

}
