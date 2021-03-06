/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.drops;

import java.util.ArrayList;
import java.util.Collection;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;

/**
 * @author NosBit
 */
public class GeneralDropItem implements IDropItem
{
	private final int _itemId;
	private final long _min;
	private final long _max;
	private final double _chance;
	
	/**
	 * @param itemId the item id
	 * @param min the min count
	 * @param max the max count
	 * @param chance the chance of this drop item
	 */
	public GeneralDropItem(int itemId, long min, long max, double chance)
	{
		_itemId = itemId;
		_min = min;
		_max = max;
		_chance = chance;
	}
	
	protected double getGlobalChanceMultiplier()
	{
		return 1.;
	}
	
	protected double getGlobalAmountMultiplier()
	{
		return 1.;
	}
	
	private final long getMinMax(L2Character victim, L2Character killer, long val)
	{
		double multiplier = 1;
		
		// individual drop amount
		Float individualDropAmountMultiplier = Config.RATE_DROP_AMOUNT_MULTIPLIER.get(getItemId());
		if (individualDropAmountMultiplier != null)
		{
			// individual amount list multiplier
			multiplier *= individualDropAmountMultiplier;
		}
		else
		{
			final L2Item item = ItemTable.getInstance().getTemplate(getItemId());
			// global amount multiplier
			if ((item != null) && item.hasExImmediateEffect())
			{
				// global herb amount multiplier
				multiplier *= Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
			}
			else
			{
				// drop type specific amount multiplier
				multiplier *= getGlobalAmountMultiplier();
			}
		}
		
		// global champions amount multiplier
		if (victim.isChampion())
		{
			multiplier *= getItemId() != Inventory.ADENA_ID ? Config.L2JMOD_CHAMPION_REWARDS : Config.L2JMOD_CHAMPION_ADENAS_REWARDS;
		}
		
		return (long) (val * multiplier);
	}
	
	/**
	 * Gets the item id
	 * @return the item id
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Gets the min drop count
	 * @return the min
	 */
	public long getMin()
	{
		return _min;
	}
	
	/**
	 * Gets the min drop count
	 * @param victim the victim
	 * @param killer the killer
	 * @return the min modified by any rates.
	 */
	public long getMin(L2Character victim, L2Character killer)
	{
		return getMinMax(victim, killer, getMin());
	}
	
	/**
	 * Gets the max drop count
	 * @return the max
	 */
	public long getMax()
	{
		return _max;
	}
	
	/**
	 * Gets the max drop count
	 * @param victim the victim
	 * @param killer the killer
	 * @return the max modified by any rates.
	 */
	public long getMax(L2Character victim, L2Character killer)
	{
		return getMinMax(victim, killer, getMax());
	}
	
	/**
	 * Gets the chance of this drop item.
	 * @return the chance
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Gets the chance of this drop item.
	 * @param victim the victim
	 * @param killer the killer
	 * @return the chance modified by any rates.
	 */
	public double getChance(L2Character victim, L2Character killer)
	{
		double multiplier = 1;
		
		// individual drop chance
		Float individualDropChanceMultiplier = Config.RATE_DROP_CHANCE_MULTIPLIER.get(getItemId());
		if (individualDropChanceMultiplier != null)
		{
			multiplier *= individualDropChanceMultiplier;
		}
		else
		{
			final L2Item item = ItemTable.getInstance().getTemplate(getItemId());
			if ((item != null) && item.hasExImmediateEffect())
			{
				multiplier *= Config.RATE_HERB_DROP_CHANCE_MULTIPLIER;
			}
			else
			{
				multiplier *= getGlobalChanceMultiplier();
			}
		}
		
		if (victim.isChampion())
		{
			multiplier *= Config.L2JMOD_CHAMPION_REWARDS;
		}
		
		return (getChance() * multiplier);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.drop.IDropItem#calculateDrops(com.l2jserver.gameserver.model.actor.L2Character, com.l2jserver.gameserver.model.actor.L2Character)
	 */
	@Override
	public Collection<ItemHolder> calculateDrops(L2Character victim, L2Character killer)
	{
		final int levelDifference = victim.getLevel() - killer.getLevel();
		final double levelGapChanceToDrop;
		if (getItemId() == Inventory.ADENA_ID)
		{
			levelGapChanceToDrop = Util.map(levelDifference, -Config.DROP_ADENA_MAX_LEVEL_DIFFERENCE, -Config.DROP_ADENA_MIN_LEVEL_DIFFERENCE, Config.DROP_ADENA_MIN_LEVEL_GAP_CHANCE, 100.0);
		}
		else
		{
			levelGapChanceToDrop = Util.map(levelDifference, -Config.DROP_ITEM_MAX_LEVEL_DIFFERENCE, -Config.DROP_ITEM_MIN_LEVEL_DIFFERENCE, Config.DROP_ITEM_MIN_LEVEL_GAP_CHANCE, 100.0);
		}
		
		// There is a chance of level gap that it wont drop this item
		if (levelGapChanceToDrop < (Rnd.nextDouble() * 100))
		{
			return null;
		}
		
		final double chance = getChance(victim, killer);
		int successes;
		if (!Config.L2JMOD_OLD_DROP_BEHAVIOR)
		{
			successes = chance > (Rnd.nextDouble() * 100) ? 1 : 0;
		}
		else
		{
			successes = (int) (chance / 100);
			successes += (chance % 100) > (Rnd.nextDouble() * 100) ? 1 : 0;
		}
		
		if (successes > 0)
		{
			final Collection<ItemHolder> items = new ArrayList<>(1);
			items.add(new ItemHolder(getItemId(), Rnd.get(getMin(victim, killer), getMax(victim, killer)) * successes));
			return items;
		}
		
		return null;
	}
}
