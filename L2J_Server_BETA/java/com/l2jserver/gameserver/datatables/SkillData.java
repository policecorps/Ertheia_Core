/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.datatables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentEngine;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.skills.Skill;

/**
 * Skill data.
 */
public final class SkillData
{
	private static Logger _log = Logger.getLogger(SkillData.class.getName());
	
	private final Map<Integer, Skill> _skills = new HashMap<>();
	private final Map<Integer, Integer> _skillMaxLevel = new HashMap<>();
	private final List<Integer> _enchantable = new ArrayList<>();
	
	protected SkillData()
	{
		load();
	}
	
	public void reload()
	{
		load();
		// Reload Skill Tree as well.
		SkillTreesData.getInstance().load();
	}
	
	private void load()
	{
		_skills.clear();
		DocumentEngine.getInstance().loadAllSkills(_skills);
		
		_skillMaxLevel.clear();
		for (Skill skill : _skills.values())
		{
			final int skillId = skill.getId();
			final int skillLvl = skill.getLevel();
			if (skillLvl > 99)
			{
				if (!_enchantable.contains(skillId))
				{
					_enchantable.add(skillId);
				}
				continue;
			}
			
			// only non-enchanted skills
			final int maxLvl = getMaxLevel(skillId);
			if (skillLvl > maxLvl)
			{
				_skillMaxLevel.put(skillId, skillLvl);
			}
		}
		
		// Sorting for binary-search.
		Collections.sort(_enchantable);
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 1021) + skillLevel;
	}
	
	public Skill getSkill(int skillId, int level)
	{
		final Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
		{
			return result;
		}
		
		// skill/level not found, fix for transformation scripts
		final int maxLvl = getMaxLevel(skillId);
		// requested level too high
		if ((maxLvl > 0) && (level > maxLvl))
		{
			if (Config.DEBUG)
			{
				_log.log(Level.WARNING, getClass().getSimpleName() + ": call to unexisting skill level id: " + skillId + " requested level: " + level + " max level: " + maxLvl, new Throwable());
			}
			return _skills.get(getSkillHashCode(skillId, maxLvl));
		}
		
		_log.warning(getClass().getSimpleName() + ": No skill info found for skill id " + skillId + " and skill level " + level + ".");
		return null;
	}
	
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillMaxLevel.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}
	
	/**
	 * Verifies if the given skill ID correspond to an enchantable skill.
	 * @param skillId the skill ID
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise
	 */
	public boolean isEnchantable(int skillId)
	{
		return Collections.binarySearch(_enchantable, skillId) >= 0;
	}
	
	/**
	 * @param addNoble
	 * @param hasCastle
	 * @return an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public Skill[] getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		Skill[] temp = new Skill[2 + (addNoble ? 1 : 0) + (hasCastle ? 2 : 0)];
		int i = 0;
		temp[i++] = _skills.get(SkillData.getSkillHashCode(246, 1));
		temp[i++] = _skills.get(SkillData.getSkillHashCode(247, 1));
		
		if (addNoble)
		{
			temp[i++] = _skills.get(SkillData.getSkillHashCode(326, 1));
		}
		if (hasCastle)
		{
			temp[i++] = _skills.get(SkillData.getSkillHashCode(844, 1));
			temp[i++] = _skills.get(SkillData.getSkillHashCode(845, 1));
		}
		return temp;
	}
	
	/**
	 * Enum to hold some important references to frequently used (hardcoded) skills in core
	 * @author DrHouse
	 */
	public static enum FrequentSkill
	{
		RAID_CURSE(4215, 1),
		RAID_CURSE2(4515, 1),
		SEAL_OF_RULER(246, 1),
		BUILD_HEADQUARTERS(247, 1),
		WYVERN_BREATH(4289, 1),
		STRIDER_SIEGE_ASSAULT(325, 1),
		FIREWORK(5965, 1),
		LARGE_FIREWORK(2025, 1),
		BLESSING_OF_PROTECTION(5182, 1),
		VOID_BURST(3630, 1),
		VOID_FLOW(3631, 1),
		THE_VICTOR_OF_WAR(5074, 1),
		THE_VANQUISHED_OF_WAR(5075, 1),
		SPECIAL_TREE_RECOVERY_BONUS(2139, 1),
		WEAPON_GRADE_PENALTY(6209, 1),
		ARMOR_GRADE_PENALTY(6213, 1);
		
		private final SkillHolder _holder;
		
		private FrequentSkill(int id, int level)
		{
			_holder = new SkillHolder(id, level);
		}
		
		public int getId()
		{
			return _holder.getSkillId();
		}
		
		public int getLevel()
		{
			return _holder.getSkillLvl();
		}
		
		public Skill getSkill()
		{
			return _holder.getSkill();
		}
	}
	
	public static SkillData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData _instance = new SkillData();
	}
}