/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package controllers.chqbll;

import meta.RightBind;
import play.mvc.Controller;
import play.mvc.Result;
import enums.ChqbllSort;

/**
 * @author mdpinar
*/
public class TransForBill extends Controller {

	public static Result list(RightBind rightBind) {
		return Trans.list(ChqbllSort.Bill, rightBind);
	}

	public static Result save(RightBind rightBind) {
		return Trans.save(ChqbllSort.Bill, rightBind);
	}

	public static Result create(RightBind rightBind) {
		return Trans.create(ChqbllSort.Bill, rightBind);
	}

	public static Result edit(Integer id, RightBind rightBind) {
		return Trans.edit(id, ChqbllSort.Bill, rightBind);
	}

	public static Result remove(Integer id, RightBind rightBind) {
		return Trans.remove(id, ChqbllSort.Bill, rightBind);
	}

}
