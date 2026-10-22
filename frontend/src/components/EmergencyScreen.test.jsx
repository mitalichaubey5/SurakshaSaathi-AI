import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import EmergencyScreen from './EmergencyScreen';
import { strings } from '../i18n/strings';

const guideline = {
  headline: 'If you have been cheated — do these right now',
  helplines: [
    { name: 'National Cyber Crime Helpline', number: '1930', whenToUse: 'Call 1930 to report financial fraud (24x7)' },
    { name: 'Police Emergency', number: '112', whenToUse: 'Call 112 when in danger' },
  ],
  immediateSteps: ['Do NOT click any link.', 'Do NOT share OTP.'],
  reportUrl: 'https://cybercrime.gov.in',
  slogan: 'Pause. Verify. Then Trust.',
};

describe('EmergencyScreen', () => {
  it('shows helplines as tappable tel: links and the official report portal', () => {
    render(
      <EmergencyScreen t={strings.en} guideline={guideline} busy={false} error="" onRetry={() => {}} onBack={() => {}} />,
    );

    const helpline = screen.getByRole('link', { name: /National Cyber Crime Helpline/ });
    expect(helpline).toHaveAttribute('href', 'tel:1930');
    expect(screen.getByText('1930')).toBeInTheDocument();
    expect(screen.getByText(/report financial fraud/)).toBeInTheDocument();

    const report = screen.getByRole('link', { name: strings.en.openReportSite });
    expect(report).toHaveAttribute('href', 'https://cybercrime.gov.in');
  });

  it('lists every immediate step', () => {
    render(
      <EmergencyScreen t={strings.en} guideline={guideline} busy={false} error="" onRetry={() => {}} onBack={() => {}} />,
    );
    expect(screen.getByText('Do NOT click any link.')).toBeInTheDocument();
    expect(screen.getByText('Do NOT share OTP.')).toBeInTheDocument();
  });

  it('shows a loading message while fetching', () => {
    render(
      <EmergencyScreen t={strings.en} guideline={null} busy error="" onRetry={() => {}} onBack={() => {}} />,
    );
    expect(screen.getByText(strings.en.emergencyLoading, { exact: false })).toBeInTheDocument();
  });

  it('shows the error with a working Try Again action', () => {
    const onRetry = vi.fn();
    render(
      <EmergencyScreen t={strings.en} guideline={null} busy={false} error={strings.en.emergencyError} onRetry={onRetry} onBack={() => {}} />,
    );
    expect(screen.getByText(strings.en.emergencyError)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Try Again/ }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});