import { create } from 'zustand';

const initialLeads = [
  {
    id: '1',
    name: 'Sarah Connor',
    company: 'SkyNet Solutions',
    email: 'sarah.c@skynetsolutions.com',
    phone: '+1 (555) 019-2831',
    dealValue: 125000,
    pipelineStage: 'In Progress',
    priority: 'High',
    notes: 'Key contact for the automated defense platform upgrade project. Schedule a follow-up demo next week.',
    createdAt: 1718641180000
  },
  {
    id: '2',
    name: 'John Doe',
    company: 'Acme Corp',
    email: 'john.doe@acme.com',
    phone: '+1 (555) 014-9988',
    dealValue: 45000,
    pipelineStage: 'Lead',
    priority: 'Medium',
    notes: 'Interested in bulk licensing terms. Sent initial pricing sheet.',
    createdAt: 1718640180000
  },
  {
    id: '3',
    name: 'Bruce Wayne',
    company: 'Wayne Enterprises',
    email: 'bruce@waynecorp.com',
    phone: '+1 (555) 007-1939',
    dealValue: 950000,
    pipelineStage: 'Won',
    priority: 'High',
    notes: 'Closing contract for security infrastructure deployment. Final signatures pending approval.',
    createdAt: 1718639180000
  },
  {
    id: '4',
    name: 'Diana Prince',
    company: 'Themyscira Exports',
    email: 'diana@themyscira.org',
    phone: '+1 (555) 012-3456',
    dealValue: 75000,
    pipelineStage: 'Contacted',
    priority: 'Low',
    notes: 'Discussed historical artifact preservation software. Follow up in late June.',
    createdAt: 1718638180000
  },
  {
    id: '5',
    name: 'Clark Kent',
    company: 'Daily Planet',
    email: 'clark.kent@dailyplanet.com',
    phone: '+1 (555) 098-7654',
    dealValue: 12000,
    pipelineStage: 'Lead',
    priority: 'Low',
    notes: 'Requested informational brochures on CRM portability. Low urgency.',
    createdAt: 1718637180000
  },
  {
    id: '6',
    name: 'Tony Stark',
    company: 'Stark Industries',
    email: 'tony@starkindustries.com',
    phone: '+1 (555) 300-3000',
    dealValue: 1500000,
    pipelineStage: 'In Progress',
    priority: 'High',
    notes: 'Custom pipeline dashboard and webhook API integration support needed. High-value enterprise account.',
    createdAt: 1718636180000
  }
];

export const useLeadStore = create((set) => ({
  leads: initialLeads,
  
  addLead: (lead) => set((state) => ({
    leads: [...state.leads, { ...lead, id: lead.id || Date.now().toString(), createdAt: Date.now() }]
  })),

  updateLead: (id, updatedFields) => set((state) => ({
    leads: state.leads.map((lead) => (lead.id === id ? { ...lead, ...updatedFields } : lead))
  })),

  deleteLead: (id) => set((state) => ({
    leads: state.leads.filter((lead) => lead.id !== id)
  })),

  setLeads: (leads) => set({ leads })
}));
