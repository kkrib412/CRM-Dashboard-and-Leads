import React, { useState } from 'react';
import { useLeadStore } from '../store/useLeadStore';

const STAGES = [
  'New Lead',
  'Contacted',
  'Proposal Sent',
  'In Negotiation',
  'Won',
  'Lost'
];

const STAGE_COLORS = {
  'New Lead': 'border-l-4 border-blue-500 bg-blue-500/10 text-blue-400',
  'Contacted': 'border-l-4 border-cyan-500 bg-cyan-500/10 text-cyan-400',
  'Proposal Sent': 'border-l-4 border-purple-500 bg-purple-500/10 text-purple-400',
  'In Negotiation': 'border-l-4 border-amber-500 bg-amber-500/10 text-amber-400',
  'Won': 'border-l-4 border-emerald-500 bg-emerald-500/10 text-emerald-400',
  'Lost': 'border-l-4 border-rose-500 bg-rose-500/10 text-rose-400'
};

const PRIORITY_COLORS = {
  'High': 'bg-rose-500/20 text-rose-400 border border-rose-500/30',
  'Medium': 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
  'Low': 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
};

export default function KanbanBoard() {
  const { leads, updateLead, deleteLead } = useLeadStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [draggedLeadId, setDraggedLeadId] = useState(null);
  const [activeDropColumn, setActiveDropColumn] = useState(null);

  // Currency utility
  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 0
    }).format(value);
  };

  // Drag and Drop Handlers
  const handleDragStart = (e, leadId) => {
    setDraggedLeadId(leadId);
    e.dataTransfer.setData('text/plain', leadId);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragEnd = () => {
    setDraggedLeadId(null);
    setActiveDropColumn(null);
  };

  const handleDragOver = (e, stage) => {
    e.preventDefault();
    if (activeDropColumn !== stage) {
      setActiveDropColumn(stage);
    }
  };

  const handleDrop = (e, targetStage) => {
    e.preventDefault();
    const leadId = e.dataTransfer.getData('text/plain') || draggedLeadId;
    if (leadId) {
      updateLead(leadId, { pipelineStage: targetStage });
    }
    setDraggedLeadId(null);
    setActiveDropColumn(null);
  };

  // Filter leads based on search query
  const filteredLeads = leads.filter(lead => {
    const query = searchQuery.toLowerCase();
    return (
      lead.name.toLowerCase().includes(query) ||
      lead.company.toLowerCase().includes(query) ||
      (lead.notes && lead.notes.toLowerCase().includes(query))
    );
  });

  // Calculate stats
  const totalValue = filteredLeads.reduce((acc, curr) => acc + (curr.dealValue || 0), 0);

  return (
    <div className="space-y-6">
      {/* Visual Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white mb-1">Pipeline Board</h2>
          <p className="text-gray-400 text-sm">
            Drag and drop cards across stages to instantly update lead status.
          </p>
        </div>

        {/* Board Search and Counter */}
        <div className="flex items-center gap-4 flex-wrap">
          <div className="relative">
            <input
              type="text"
              placeholder="Search by name, company..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-[#111827] text-white border border-[#1F2937] px-4 py-2 pl-10 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#A855F7] focus:border-transparent w-64 transition-all duration-200"
            />
            <div className="absolute left-3 top-2.5 text-gray-400">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-2.5 text-gray-400 hover:text-white"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>

          <div className="px-4 py-2 bg-[#111827] border border-[#1F2937] rounded-lg text-sm font-semibold flex items-center gap-2">
            <span className="text-gray-400">Total Active Portfolio:</span>
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#A855F7] to-[#EC4899] font-bold">
              {formatCurrency(totalValue)}
            </span>
          </div>
        </div>
      </div>

      {/* Horizontal Scroll Kanban Container */}
      <div className="overflow-x-auto pb-4 -mx-4 px-4 md:-mx-8 md:px-8 lg:-mx-10 lg:px-10 scrollbar-thin scrollbar-thumb-gray-800 scrollbar-track-transparent">
        <div className="flex gap-4 min-w-[1200px] h-[calc(100vh-220px)] min-h-[500px]">
          {STAGES.map((stage) => {
            const stageLeads = filteredLeads.filter(lead => {
              // Normalize stage names just in case there are mismatches
              const normalizedLeadStage = lead.pipelineStage === 'In Progress' || lead.pipelineStage === 'Lead' || lead.pipelineStage === 'Contacted'
                ? (lead.pipelineStage === 'Lead' ? 'New Lead' : lead.pipelineStage === 'In Progress' ? 'In Negotiation' : lead.pipelineStage)
                : lead.pipelineStage;
              return normalizedLeadStage === stage;
            });
            const columnValue = stageLeads.reduce((acc, curr) => acc + (curr.dealValue || 0), 0);
            const isTargeted = activeDropColumn === stage;

            return (
              <div
                key={stage}
                onDragOver={(e) => handleDragOver(e, stage)}
                onDragLeave={() => setActiveDropColumn(null)}
                onDrop={(e) => handleDrop(e, stage)}
                className={`flex-1 flex flex-col bg-[#111827]/40 border border-[#1F2937] rounded-xl p-3 Transition-all duration-300 ${
                  isTargeted ? 'bg-[#1F2937]/30 border-[#A855F7] scale-[1.01]' : ''
                }`}
              >
                {/* Column Title Header */}
                <div className={`flex items-center justify-between p-2 mb-3 rounded-lg ${STAGE_COLORS[stage] || 'border-l-4 border-gray-500'}`}>
                  <div className="font-bold text-xs uppercase tracking-wider flex items-center gap-1.5 label_stage">
                    {stage}
                    <span className="bg-black/20 text-[10px] px-1.5 py-0.5 rounded-full">
                      {stageLeads.length}
                    </span>
                  </div>
                  <span className="text-[11px] font-bold opacity-80">
                    {formatCurrency(columnValue)}
                  </span>
                </div>

                {/* Lead Cards List Container */}
                <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 scrollbar-thin scrollbar-thumb-gray-800 scrollbar-track-transparent">
                  {stageLeads.length === 0 ? (
                    <div className="h-28 border border-dashed border-[#1F2937] rounded-lg flex flex-col items-center justify-center text-center p-4">
                      <p className="text-xs text-gray-500 font-medium">No leads in stage</p>
                    </div>
                  ) : (
                    stageLeads.map((lead) => (
                      <div
                        key={lead.id}
                        draggable
                        onDragStart={(e) => handleDragStart(e, lead.id)}
                        onDragEnd={handleDragEnd}
                        className={`group relative bg-[#111827] border border-[#1F2937] hover:border-[#1F2937]/80 hover:shadow-lg hover:shadow-black/20 p-4 rounded-xl cursor-grab active:cursor-grabbing transition-all duration-200 ${
                          draggedLeadId === lead.id ? 'opacity-40 scale-95 border-dashed border-gray-600' : ''
                        }`}
                      >
                        {/* Drag Handle Indicator */}
                        <div className="absolute top-4 right-4 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                          <button
                            onClick={() => deleteLead(lead.id)}
                            className="p-1 hover:bg-rose-500/10 hover:text-rose-400 rounded text-gray-500 transition-all duration-200"
                            title="Delete Lead"
                          >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                          <div className="text-gray-500 cursor-grab p-1 hover:text-gray-300">
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8h16M4 16h16" />
                            </svg>
                          </div>
                        </div>

                        {/* Title and Company */}
                        <div className="mb-2">
                          <h4 className="font-bold text-sm text-white leading-snug group-hover:text-transparent group-hover:bg-clip-text group-hover:bg-gradient-to-r group-hover:from-white group-hover:to-zinc-300 transition-all duration-200">
                            {lead.name}
                          </h4>
                          <p className="text-xs text-gray-400 font-medium truncate mt-0.5 max-w-[85%]">
                            {lead.company}
                          </p>
                        </div>

                        {/* Deal Value */}
                        <div className="font-bold text-sm text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-300 mb-3">
                          {formatCurrency(lead.dealValue)}
                        </div>

                        {/* Card Footer Info */}
                        <div className="flex items-center justify-between gap-2 border-t border-[#1F2937]/50 pt-2.5 mt-2.5">
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md uppercase tracking-wider ${PRIORITY_COLORS[lead.priority] || 'bg-gray-800 text-gray-400'}`}>
                            {lead.priority}
                          </span>
                          
                          {/* Display short date snippet */}
                          <span className="text-[10px] text-gray-500 font-semibold">
                            {new Date(lead.createdAt || Date.now()).toLocaleDateString('en-US', {
                              month: 'short',
                              day: 'numeric'
                            })}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
