/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
/**
 * @author mdpinar
*/
public class StockTransFactor extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public Integer id;

	@ManyToOne
	public StockTrans trans;

	@ManyToOne
	public StockCostFactor factor;

	@Constraints.Required
	public Double effect;

	@Constraints.Required
	public Double amount;

}
