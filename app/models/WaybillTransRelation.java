/*
 * Copyright 2014 Mustafa DUMLUPINAR
 * 
 * mdumlupinar@gmail.com
 * 
*/

package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
/**
 * @author mdpinar
*/
public class WaybillTransRelation extends AbstractStockTransRelation {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	public WaybillTrans trans;

}
