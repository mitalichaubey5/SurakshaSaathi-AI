import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import RiskBadge from './RiskBadge';
import { strings } from '../i18n/strings';

describe('RiskBadge', () => {
  const t = strings.en;

  it('shows the label as text (not just colour) for each risk level', () => {
    const cases = [
      { level: 'HIGH', label: 'High Risk', emoji: '🔴' },
      { level: 'MEDIUM', label: 'Needs Verification', emoji: '🟡' },
      { level: 'LOW', label: 'Low Risk', emoji: '🟢' },
    ];
    for (const c of cases) {
      const { unmount } = render(<RiskBadge riskLevel={c.level} riskLabel={c.label} riskEmoji={c.emoji} t={t} />);
      expect(screen.getByText(c.label)).toBeInTheDocument();
      expect(screen.getByText(c.emoji)).toBeInTheDocument();
      unmount();
    }
  });

  it('announces the result to assistive technology', () => {
    render(<RiskBadge riskLevel="HIGH" riskLabel="High Risk" riskEmoji="🔴" t={t} />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});